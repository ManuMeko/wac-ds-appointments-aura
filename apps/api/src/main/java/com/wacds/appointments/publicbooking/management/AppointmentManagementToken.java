
package com.wacds.appointments.publicbooking.management;

import com.wacds.appointments.appointment.Appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Credencial privada de gestion publica de reservas.
 *
 * SEGURIDAD
 *
 * - No almacena el token original.
 * - Almacena exclusivamente SHA-256 hexadecimal.
 * - Cada token pertenece a una Appointment.
 * - El hash debe contener 64 caracteres hexadecimales.
 * - Una credencial revocada no puede utilizarse.
 * - La expiracion se compara mediante el instante real.
 * - Las operaciones de dominio respetan la auditoria.
 *
 * CONCURRENCIA
 *
 * Esta entidad no adquiere bloqueos.
 *
 * AppointmentManagementTokenService y su repositorio
 * controlan las operaciones concurrentes.
 *
 * RESERVAS
 *
 * PublicBookingService genera la credencial
 * despues de crear la Appointment.
 *
 * AISLAMIENTO
 *
 * La pertenencia al negocio se determina
 * mediante la Appointment asociada.
 *
 * BASE DE DATOS
 *
 * Se mantienen:
 *
 * - Nombre de tabla.
 * - Columnas.
 * - Relaciones.
 * - Indices.
 * - Restriccion unica del hash.
 *
 * No requiere migraciones Flyway.
 *
 * IMPORTANTE
 *
 * Esta entidad no configura eliminacion
 * en cascada desde JPA.
 *
 * La limpieza de datos de prueba debe
 * respetar las claves foraneas existentes.
 */
@Entity
@Table(
    name = "appointment_management_tokens",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_appointment_management_tokens_token_hash",
            columnNames = {"token_hash"}
        )
    },
    indexes = {
        @Index(
            name = "idx_appointment_management_tokens_appointment_id",
            columnList = "appointment_id"
        ),
        @Index(
            name = "idx_appointment_management_tokens_expires_at",
            columnList = "expires_at"
        ),
        @Index(
            name = "idx_appointment_management_tokens_revoked_at",
            columnList = "revoked_at"
        ),
        @Index(
            name = "idx_appointment_management_tokens_active_lookup",
            columnList = "appointment_id, revoked_at, expires_at"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentManagementToken {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final Pattern SHA256_PATTERN =
        Pattern.compile("^[0-9a-f]{64}$");

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    // =========================================================
    // APPOINTMENT
    // =========================================================

    /**
     * Cita asociada a esta credencial.
     *
     * La cita debe estar persistida.
     *
     * No se utiliza CascadeType.REMOVE
     * ni orphanRemoval.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "appointment_id",
        nullable = false
    )
    private Appointment appointment;

    // =========================================================
    // TOKEN HASH
    // =========================================================

    /**
     * SHA-256 hexadecimal del token.
     *
     * El token original nunca debe guardarse.
     *
     * Se mantiene VARCHAR(128) por compatibilidad,
     * aunque el hash validado tiene 64 caracteres.
     */
    @Column(
        name = "token_hash",
        nullable = false,
        length = 128,
        unique = true
    )
    private String tokenHash;

    // =========================================================
    // EXPIRATION
    // =========================================================

    /**
     * Caducidad de la credencial.
     *
     * null conserva el comportamiento existente:
     * sin caducidad tecnica explicita.
     *
     * El servicio de emision determina
     * la fecha de caducidad de tokens nuevos.
     */
    @Column(
        name = "expires_at"
    )
    private OffsetDateTime expiresAt;

    // =========================================================
    // REVOCATION
    // =========================================================

    /**
     * Fecha de revocacion.
     *
     * null significa que no existe revocacion.
     */
    @Column(
        name = "revoked_at"
    )
    private OffsetDateTime revokedAt;

    // =========================================================
    // LAST USE
    // =========================================================

    /**
     * Ultimo uso registrado.
     *
     * Se mantiene monotonicamente:
     * no retrocede cuando se registra
     * un uso anterior.
     */
    @Column(
        name = "last_used_at"
    )
    private OffsetDateTime lastUsedAt;

    // =========================================================
    // AUDIT
    // =========================================================

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private OffsetDateTime updatedAt;

    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    /**
     * Valida el token antes de insertarlo.
     *
     * Inicializa auditoria antes
     * de comprobar su consistencia.
     */
    @PrePersist
    void prePersist() {

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (
            updatedAt == null
                || updatedAt.isBefore(createdAt)
        ) {
            updatedAt = createdAt;
        }

        validateEntityState();

        updatedAt = now.isAfter(createdAt)
            ? now
            : createdAt;
    }

    /**
     * Comprueba la entidad antes de actualizar.
     *
     * Las actualizaciones masivas JPQL
     * no ejecutan necesariamente @PreUpdate.
     *
     * AppointmentManagementTokenRepository
     * debe actualizar explicitamente updatedAt
     * cuando realiza operaciones masivas.
     */
    @PreUpdate
    void preUpdate() {

        validateEntityState();

        OffsetDateTime now = OffsetDateTime.now();

        if (
            updatedAt == null
                || now.isAfter(updatedAt)
        ) {
            updatedAt = now;
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Indica si existe revocacion.
     */
    public boolean isRevoked() {

        return revokedAt != null;
    }

    /**
     * Indica si el token esta caducado
     * en el instante proporcionado.
     *
     * La expiracion es inclusiva:
     * en expiresAt el token ya ha caducado.
     *
     * Si no se proporciona now,
     * se utiliza el instante actual.
     */
    public boolean isExpired(
        OffsetDateTime now
    ) {

        if (expiresAt == null) {
            return false;
        }

        OffsetDateTime referenceTime =
            Objects.requireNonNullElseGet(
                now,
                OffsetDateTime::now
            );

        return !expiresAt.isAfter(referenceTime);
    }

    /**
     * Comprobacion tecnica de vigencia.
     *
     * No sustituye la autorizacion completa
     * del servicio de gestion de reservas.
     */
    public boolean isUsable(
        OffsetDateTime now
    ) {

        return !isRevoked()
            && !isExpired(now);
    }

    // =========================================================
    // REVOCATION
    // =========================================================

    /**
     * Revoca el token en el instante actual.
     */
    public void revoke() {

        revoke(OffsetDateTime.now());
    }

    /**
     * Revocacion idempotente.
     *
     * Una credencial ya revocada
     * conserva su fecha original.
     *
     * No se permite revocar con fecha
     * anterior a createdAt.
     */
    public void revoke(
        OffsetDateTime revokedAt
    ) {

        if (this.revokedAt != null) {
            return;
        }

        if (revokedAt == null) {
            throw new IllegalArgumentException(
                "revokedAt es obligatorio para revocar el token."
            );
        }

        if (
            createdAt != null
                && revokedAt.isBefore(createdAt)
        ) {
            throw new IllegalArgumentException(
                "revokedAt no puede ser anterior a createdAt."
            );
        }

        if (
            lastUsedAt != null
                && revokedAt.isBefore(lastUsedAt)
        ) {
            throw new IllegalArgumentException(
                "revokedAt no puede ser anterior a lastUsedAt."
            );
        }

        this.revokedAt = revokedAt;
    }

    // =========================================================
    // LAST USE
    // =========================================================

    /**
     * Registra un uso con la fecha actual.
     */
    public void markUsed() {

        markUsed(OffsetDateTime.now());
    }

    /**
     * Registra el ultimo uso valido del token.
     *
     * No permite:
     *
     * - Uso de credenciales revocadas.
     * - Uso de credenciales expiradas.
     * - Uso anterior a la creacion.
     *
     * Si la credencial ya tiene un uso
     * posterior, conserva ese instante.
     */
    public void markUsed(
        OffsetDateTime usedAt
    ) {

        if (usedAt == null) {
            throw new IllegalArgumentException(
                "usedAt es obligatorio."
            );
        }

        if (isRevoked()) {
            throw new IllegalStateException(
                "No se puede registrar el uso "
                    + "de una credencial revocada."
            );
        }

        if (isExpired(usedAt)) {
            throw new IllegalStateException(
                "No se puede registrar el uso "
                    + "de una credencial expirada."
            );
        }

        if (
            createdAt != null
                && usedAt.isBefore(createdAt)
        ) {
            throw new IllegalArgumentException(
                "lastUsedAt no puede ser anterior a createdAt."
            );
        }

        if (
            lastUsedAt == null
                || usedAt.isAfter(lastUsedAt)
        ) {
            lastUsedAt = usedAt;
        }
    }

    // =========================================================
    // ENTITY VALIDATION
    // =========================================================

    /**
     * Valida la integridad estructural
     * y temporal del token.
     */
    private void validateEntityState() {

        validateAppointment();

        validateTokenHash();

        validateTimestamps();
    }

    // =========================================================
    // APPOINTMENT VALIDATION
    // =========================================================

    private void validateAppointment() {

        if (
            appointment == null
                || appointment.getId() == null
                || appointment.getId() <= 0
        ) {
            throw new IllegalStateException(
                "AppointmentManagementToken debe "
                    + "pertenecer a una cita persistida."
            );
        }
    }

    // =========================================================
    // TOKEN HASH VALIDATION
    // =========================================================

    /**
     * Normaliza el hash a minusculas
     * y valida que sea SHA-256 hexadecimal.
     */
    private void validateTokenHash() {

        if (
            tokenHash == null
                || tokenHash.isBlank()
        ) {
            throw new IllegalStateException(
                "tokenHash es obligatorio."
            );
        }

        String normalizedHash =
            tokenHash.trim().toLowerCase(
                Locale.ROOT
            );

        if (
            !SHA256_PATTERN.matcher(
                normalizedHash
            ).matches()
        ) {
            throw new IllegalStateException(
                "tokenHash debe ser un SHA-256 hexadecimal "
                    + "de 64 caracteres."
            );
        }

        tokenHash = normalizedHash;
    }

    // =========================================================
    // TIMESTAMP VALIDATION
    // =========================================================

    private void validateTimestamps() {

        if (createdAt == null) {
            throw new IllegalStateException(
                "createdAt es obligatorio."
            );
        }

        if (
            expiresAt != null
                && expiresAt.isBefore(createdAt)
        ) {
            throw new IllegalStateException(
                "expiresAt no puede ser anterior a createdAt."
            );
        }

        if (
            revokedAt != null
                && revokedAt.isBefore(createdAt)
        ) {
            throw new IllegalStateException(
                "revokedAt no puede ser anterior a createdAt."
            );
        }

        if (
            lastUsedAt != null
                && lastUsedAt.isBefore(createdAt)
        ) {
            throw new IllegalStateException(
                "lastUsedAt no puede ser anterior a createdAt."
            );
        }

        if (
            revokedAt != null
                && lastUsedAt != null
                && lastUsedAt.isAfter(revokedAt)
        ) {
            throw new IllegalStateException(
                "lastUsedAt no puede ser posterior a revokedAt."
            );
        }

        if (
            updatedAt != null
                && updatedAt.isBefore(createdAt)
        ) {
            throw new IllegalStateException(
                "updatedAt no puede ser anterior a createdAt."
            );
        }
    }
}
