package com.wacds.appointments.publicbooking.management;
import com.wacds.appointments.appointment.Appointment;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
/**
 * Servicio de credenciales privadas de gestion de reservas.
 *
 * Contrato:
 * - Los tokens originales nunca se persisten.
 * - Unicamente se almacena SHA-256 hexadecimal.
 * - Cada token pertenece a una Appointment concreta.
 * - No se admiten credenciales revocadas o caducadas.
 * - No se emiten ni utilizan tokens de citas canceladas.
 * - Las operaciones publicas conservan sus firmas.
 *
 * La exclusion concurrente en base de datos depende
 * tambien de AppointmentManagementTokenRepository.
 */
@Service
@RequiredArgsConstructor
public class AppointmentManagementTokenService {
    // =========================================================
    // CONSTANTS
    // =========================================================
    /**
     * 32 bytes = 256 bits de entropia.
     */
    private static final int TOKEN_BYTES = 32;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final long DEFAULT_TOKEN_LIFETIME_DAYS = 180;
    /**
     * Base64 URL-safe sin padding.
     *
     * La longitud maxima admite posibles versiones futuras
     * sin permitir caracteres de control, espacios ni
     * caracteres especiales ajenos al formato.
     */
    private static final int MIN_TOKEN_LENGTH = 32;
    private static final int MAX_TOKEN_LENGTH = 256;
    private static final Pattern TOKEN_PATTERN =
        Pattern.compile("^[A-Za-z0-9_-]{32,256}$");
    private static final Pattern SHA256_PATTERN =
        Pattern.compile("^[a-f0-9]{64}$");
    private static final String INVALID_CREDENTIAL_MESSAGE =
        "La credencial de gestion de la reserva "
            + "no existe, ha expirado o ha sido revocada.";
    // =========================================================
    // DEPENDENCIES
    // =========================================================
    private final AppointmentManagementTokenRepository tokenRepository;
    /**
     * SecureRandom utiliza una fuente criptograficamente segura.
     * No se registra ni se reutiliza un valor generado.
     */
    private final SecureRandom secureRandom = new SecureRandom();
    // =========================================================
    // ISSUE TOKEN
    // =========================================================
    /**
     * Emite una credencial para una reserva persistida.
     *
     * 1. Valida la reserva.
     * 2. Comprueba que admite credenciales.
     * 3. Revoca los tokens activos anteriores.
     * 4. Genera 32 bytes aleatorios.
     * 5. Calcula SHA-256.
     * 6. Persiste exclusivamente el hash.
     * 7. Devuelve el token original una sola vez.
     *
     * Este metodo debe ejecutarse dentro de una transaccion.
     */
    @Transactional
    public String issueToken(Appointment appointment) {
        validateAppointment(appointment);
        validateAppointmentAllowsManagement(appointment);
        OffsetDateTime now = OffsetDateTime.now();
        /*
         * Estrategia: revocacion previa a la nueva emision.
         *
         * La atomicidad corresponde a la transaccion actual.
         *
         * La serializacion entre emisiones simultaneas
         * requiere bloqueo o restriccion en BD.
         */
        tokenRepository.revokeAllActiveByAppointmentId(
            appointment.getId(),
            now
        );
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        ensureTokenHashDoesNotExist(tokenHash);
        OffsetDateTime expiresAt = now.plus(
            DEFAULT_TOKEN_LIFETIME_DAYS,
            ChronoUnit.DAYS
        );
        AppointmentManagementToken entity =
            AppointmentManagementToken.builder()
                .appointment(appointment)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        AppointmentManagementToken saved =
            tokenRepository.saveAndFlush(entity);
        if (saved == null || saved.getId() == null) {
            throw new IllegalStateException(
                "No se pudo persistir la credencial de gestion."
            );
        }
        if (!tokenHash.equals(saved.getTokenHash())) {
            throw new IllegalStateException(
                "La credencial persistida no coincide "
                    + "con el hash generado."
            );
        }
        /*
         * El token original no se guarda en la entidad.
         * Quien invoca este metodo es responsable
         * de no registrarlo ni exponerlo indebidamente.
         */
        return rawToken;
    }
    // =========================================================
    // REQUIRE USABLE TOKEN
    // =========================================================
    /**
     * Recupera una credencial tecnicamente utilizable.
     *
     * El hash se calcula sobre el valor exacto recibido.
     * Nunca se utiliza el identificador de la cita
     * como sustituto de autorizacion.
     */
    @Transactional(readOnly = true)
    public AppointmentManagementToken requireUsableToken(
        String rawToken
    ) {
        validateRawToken(rawToken);
        OffsetDateTime now = OffsetDateTime.now();
        String tokenHash = hashToken(rawToken);
        AppointmentManagementToken token =
            tokenRepository.findUsableByTokenHash(
                tokenHash,
                now
            ).orElseThrow(
                () -> new ResourceNotFoundException(
                    INVALID_CREDENTIAL_MESSAGE
                )
            );
        validateTokenEntity(token, now);
        validateTokenMatchesHash(token, tokenHash);
        return token;
    }
    // =========================================================
    // REQUIRE USABLE TOKEN AND MARK USED
    // =========================================================
    /**
     * Recupera una credencial y actualiza lastUsedAt.
     *
     * Se utiliza en operaciones de modificacion.
     *
     * IMPORTANTE:
     * Esta operacion adquiere un bloqueo PESSIMISTIC_WRITE
     * sobre la fila del token hasta finalizar la transaccion.
     * La coordinacion con emisiones y revocaciones masivas
     * requiere mantener el mismo protocolo transaccional.
     */
    @Transactional
    public AppointmentManagementToken requireUsableTokenAndMarkUsed(
        String rawToken
    ) {
        validateRawToken(rawToken);
        OffsetDateTime now = OffsetDateTime.now();
        String tokenHash = hashToken(rawToken);
        AppointmentManagementToken token =
            tokenRepository.findUsableForUpdateByTokenHash(
                tokenHash,
                now
            ).orElseThrow(
                () -> new ResourceNotFoundException(
                    INVALID_CREDENTIAL_MESSAGE
                )
            );
        validateTokenEntity(token, now);
        validateTokenMatchesHash(token, tokenHash);
        token.markUsed(now);
        AppointmentManagementToken saved =
            tokenRepository.saveAndFlush(token);
        validateTokenEntity(saved, OffsetDateTime.now());
        validateTokenMatchesHash(saved, tokenHash);
        return saved;
    }
    // =========================================================
    // REQUIRE APPOINTMENT
    // =========================================================
    /**
     * Devuelve exclusivamente la Appointment asociada
     * al token recibido.
     *
     * No acepta appointmentId como mecanismo de acceso.
     */
    @Transactional(readOnly = true)
    public Appointment requireAppointment(String rawToken) {
        AppointmentManagementToken token =
            requireUsableToken(rawToken);
        Appointment appointment = token.getAppointment();
        validateAppointment(appointment);
        validateAppointmentAllowsManagement(appointment);
        return appointment;
    }
    // =========================================================
    // REVOKE INDIVIDUAL TOKEN
    // =========================================================
    /**
     * Revocacion idempotente de una credencial existente.
     *
     * No se permite revocar otra credencial mediante
     * un identificador numerico suministrado por el cliente.
     */
    @Transactional
    public void revokeToken(String rawToken) {
        validateRawToken(rawToken);
        String tokenHash = hashToken(rawToken);
        AppointmentManagementToken token =
            tokenRepository.findByTokenHash(
                tokenHash
            ).orElseThrow(
                () -> new ResourceNotFoundException(
                    INVALID_CREDENTIAL_MESSAGE
                )
            );
        validateTokenIdentity(token);
        validateTokenMatchesHash(token, tokenHash);
        if (token.isRevoked()) {
            return;
        }
        token.revoke(OffsetDateTime.now());
        tokenRepository.saveAndFlush(token);
    }
    // =========================================================
    // REVOKE ALL APPOINTMENT TOKENS
    // =========================================================
    /**
     * Revoca todos los tokens activos de una reserva.
     *
     * Se utiliza al cancelar una Appointment.
     *
     * El repositorio ejecuta la revocacion en BD.
     */
    @Transactional
    public int revokeAllForAppointment(Long appointmentId) {
        validateAppointmentId(appointmentId);
        OffsetDateTime revokedAt = OffsetDateTime.now();
        return tokenRepository.revokeAllActiveByAppointmentId(
            appointmentId,
            revokedAt
        );
    }
    // =========================================================
    // ACTIVE TOKENS
    // =========================================================
    /**
     * Recupera las entidades de credenciales utilizables
     * pertenecientes a una cita.
     *
     * Metodo interno de diagnostico y administracion.
     * No debe exponerse mediante un endpoint publico
     * basado exclusivamente en appointmentId.
     */
    @Transactional(readOnly = true)
    public List<AppointmentManagementToken> findUsableByAppointmentId(
        Long appointmentId
    ) {
        validateAppointmentId(appointmentId);
        List<AppointmentManagementToken> tokens =
            tokenRepository.findUsableByAppointmentIdOrderByCreatedAtDesc(
                appointmentId,
                OffsetDateTime.now()
            );
        /*
         * Mantener el contrato del repositorio.
         * La consulta debe filtrar expirados y revocados.
         */
        return tokens;
    }
    // =========================================================
    // GENERATE TOKEN
    // =========================================================
    /**
     * Produce 43 caracteres Base64 URL-safe sin padding
     * a partir de 32 bytes criptograficamente aleatorios.
     */
    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes);
        validateRawToken(rawToken);
        return rawToken;
    }
    // =========================================================
    // HASH TOKEN
    // =========================================================
    /**
     * Calcula SHA-256 y devuelve 64 caracteres
     * hexadecimales en minusculas.
     *
     * Se conserva el algoritmo original para
     * mantener compatibles las credenciales existentes.
     */
    public String hashToken(String rawToken) {
        validateRawToken(rawToken);
        try {
            MessageDigest digest =
                MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(
                rawToken.getBytes(StandardCharsets.UTF_8)
            );
            String encoded = HexFormat.of().formatHex(hash);
            if (!SHA256_PATTERN.matcher(encoded).matches()) {
                throw new IllegalStateException(
                    "El hash generado no tiene un formato valido."
                );
            }
            return encoded;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 no esta disponible en el entorno Java.",
                exception
            );
        }
    }
    // =========================================================
    // TOKEN ENTITY VALIDATION
    // =========================================================
    /**
     * Verifica la identidad estructural de la entidad.
     * Puede utilizarse incluso cuando ya esta revocada.
     */
    private void validateTokenIdentity(
        AppointmentManagementToken token
    ) {
        if (token == null || token.getId() == null) {
            throw new IllegalStateException(
                "La credencial recuperada no es valida."
            );
        }
        Appointment appointment = token.getAppointment();
        if (appointment == null || appointment.getId() == null) {
            throw new IllegalStateException(
                "La credencial no tiene una reserva valida."
            );
        }
        String storedHash = token.getTokenHash();
        if (
            storedHash == null ||
            !SHA256_PATTERN.matcher(storedHash).matches()
        ) {
            throw new IllegalStateException(
                "La credencial no contiene un hash SHA-256 valido."
            );
        }
    }
    /**
     * Valida estructura, vigencia, revocacion y
     * estado de la cita asociada.
     */
    private void validateTokenEntity(
        AppointmentManagementToken token,
        OffsetDateTime now
    ) {
        validateTokenIdentity(token);
        if (now == null) {
            throw new IllegalArgumentException(
                "El instante de validacion es obligatorio."
            );
        }
        if (token.isRevoked() || token.isExpired(now)) {
            throw new ResourceNotFoundException(
                INVALID_CREDENTIAL_MESSAGE
            );
        }
        Appointment appointment = token.getAppointment();
        validateAppointment(appointment);
        validateAppointmentAllowsManagement(appointment);
    }
    /**
     * Compara el hash recuperado con el esperado.
     * No compara ni almacena tokens originales.
     */
    private void validateTokenMatchesHash(
        AppointmentManagementToken token,
        String expectedHash
    ) {
        validateTokenIdentity(token);
        if (
            expectedHash == null ||
            !SHA256_PATTERN.matcher(expectedHash).matches()
        ) {
            throw new IllegalStateException(
                "El hash esperado no tiene un formato valido."
            );
        }
        byte[] actual = HexFormat.of().parseHex(
            token.getTokenHash()
        );
        byte[] expected = HexFormat.of().parseHex(
            expectedHash
        );
        if (!MessageDigest.isEqual(actual, expected)) {
            throw new ResourceNotFoundException(
                INVALID_CREDENTIAL_MESSAGE
            );
        }
    }
    // =========================================================
    // HASH COLLISION DEFENSE
    // =========================================================
    private void ensureTokenHashDoesNotExist(
        String tokenHash
    ) {
        if (
            tokenHash == null ||
            !SHA256_PATTERN.matcher(tokenHash).matches()
        ) {
            throw new IllegalArgumentException(
                "El hash de la credencial no es valido."
            );
        }
        if (tokenRepository.existsByTokenHash(tokenHash)) {
            throw new IllegalStateException(
                "Se ha producido una colision inesperada "
                    + "al generar la credencial de gestion."
            );
        }
    }
    // =========================================================
    // APPOINTMENT VALIDATION
    // =========================================================
    private void validateAppointment(
        Appointment appointment
    ) {
        if (appointment == null) {
            throw new IllegalArgumentException(
                "Appointment es obligatoria."
            );
        }
        if (appointment.getId() == null) {
            throw new IllegalArgumentException(
                "Appointment debe estar persistida antes "
                    + "de emitir una credencial de gestion."
            );
        }
        if (
            appointment.getBusiness() == null ||
            appointment.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "Appointment no tiene un Business valido."
            );
        }
        if (
            appointment.getCustomer() == null ||
            appointment.getCustomer().getId() == null
        ) {
            throw new IllegalStateException(
                "Appointment no tiene un Customer valido."
            );
        }
        if (
            appointment.getProfessional() == null ||
            appointment.getProfessional().getId() == null
        ) {
            throw new IllegalStateException(
                "Appointment no tiene un Professional valido."
            );
        }
        if (
            appointment.getService() == null ||
            appointment.getService().getId() == null
        ) {
            throw new IllegalStateException(
                "Appointment no tiene un Service valido."
            );
        }
    }
    /**
     * Defensa adicional contra credenciales emitidas
     * o reutilizadas para citas canceladas.
     *
     * Una cita CANCELLED no puede volver a gestionarse
     * mediante su antigua credencial publica.
     */
    private void validateAppointmentAllowsManagement(
        Appointment appointment
    ) {
        validateAppointment(appointment);
        if (appointment.getStatus() == null) {
            throw new IllegalStateException(
                "Appointment no tiene un estado valido."
            );
        }
        if (
            appointment.getStatus()
                == Appointment.AppointmentStatus.CANCELLED
        ) {
            throw new ConflictException(
                "No se puede gestionar una reserva cancelada."
            );
        }
    }
    // =========================================================
    // RAW TOKEN VALIDATION
    // =========================================================
    /**
     * Valida el formato sin modificar el valor original.
     *
     * No usamos trim() para transformar el token:
     * una credencial alterada debe rechazarse.
     *
     * Se conserva el rango original 32-256,
     * limitado ahora a caracteres Base64 URL-safe.
     */
    private void validateRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                "El token de gestion es obligatorio."
            );
        }
        if (
            rawToken.length() < MIN_TOKEN_LENGTH ||
            rawToken.length() > MAX_TOKEN_LENGTH
        ) {
            throw new IllegalArgumentException(
                "El token de gestion no tiene una longitud valida."
            );
        }
        if (!TOKEN_PATTERN.matcher(rawToken).matches()) {
            throw new IllegalArgumentException(
                "El token de gestion no tiene un formato valido."
            );
        }
    }
    // =========================================================
    // APPOINTMENT ID VALIDATION
    // =========================================================
    private void validateAppointmentId(Long appointmentId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException(
                "appointmentId es obligatorio."
            );
        }
        if (appointmentId <= 0) {
            throw new IllegalArgumentException(
                "appointmentId debe ser mayor que cero."
            );
        }
    }
}
