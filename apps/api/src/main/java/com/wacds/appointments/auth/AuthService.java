package com.wacds.appointments.auth;

import com.wacds.appointments.auth.dto.CurrentUserResponse;
import com.wacds.appointments.auth.dto.LoginRequest;
import com.wacds.appointments.auth.dto.LoginResponse;
import com.wacds.appointments.auth.dto.RefreshTokenRequest;
import com.wacds.appointments.auth.dto.TokenResponse;
import com.wacds.appointments.auth.membership.BusinessMembership;
import com.wacds.appointments.auth.membership.BusinessMembershipRepository;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.auth.security.JwtService;
import com.wacds.appointments.auth.token.RefreshToken;
import com.wacds.appointments.auth.token.RefreshTokenRepository;
import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int REFRESH_TOKEN_RANDOM_BYTES =
        64;

    private static final String TOKEN_TYPE =
        "Bearer";

    private static final SecureRandom SECURE_RANDOM =
        new SecureRandom();

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AuthenticationManager
        authenticationManager;

    private final UserRepository
        userRepository;

    private final BusinessMembershipRepository
        businessMembershipRepository;

    private final RefreshTokenRepository
        refreshTokenRepository;

    private final JwtService
        jwtService;

    // =========================================================
    // CONFIGURATION
    // =========================================================

    @Value(
        "${app.security.jwt.refresh-token-days:30}"
    )
    private long refreshTokenDays;

    // =========================================================
    // LOGIN
    // =========================================================

    /**
     * Autentica un usuario mediante email y password.
     *
     * Spring Security valida:
     *
     * - existencia del usuario
     * - BCrypt
     * - estado de la cuenta
     *
     * Después:
     *
     * - resolvemos su membership activa principal
     * - registramos lastLoginAt
     * - emitimos access token
     * - emitimos refresh token
     */
    @Transactional
    public LoginResponse login(
        LoginRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de login es obligatoria."
            );
        }

        String normalizedEmail =
            normalizeEmail(
                request.email()
            );

        authenticateCredentials(
            normalizedEmail,
            request.password()
        );

        User user =
            userRepository
                .findByEmailIgnoreCase(
                    normalizedEmail
                )
                .orElseThrow(() ->
                    new BadCredentialsException(
                        "Email o contraseña incorrectos."
                    )
                );

        validateActiveUser(
            user
        );

        BusinessMembership membership =
            resolvePrimaryMembership(
                user.getId()
            );

        /*
         * Delegamos el cambio de dominio a la entidad User.
         */
        user.registerSuccessfulLogin();

        userRepository.save(
            user
        );

        JwtService.GeneratedAccessToken accessToken =
            jwtService.generateAccessToken(
                user,
                membership
            );

        GeneratedRefreshToken refreshToken =
            issueRefreshToken(
                user
            );

        return new LoginResponse(
            accessToken.token(),
            refreshToken.rawToken(),
            TOKEN_TYPE,
            accessToken.expiresAt(),
            refreshToken.expiresAt(),
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            membership
                .getBusiness()
                .getId(),
            membership
                .getBusiness()
                .getName(),
            membership.getRole()
        );
    }

    // =========================================================
    // LOGIN · CREDENTIAL VALIDATION
    // =========================================================

    private void authenticateCredentials(
        String normalizedEmail,
        String password
    ) {

        try {

            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    normalizedEmail,
                    password
                )
            );

        } catch (
            org.springframework.security.core.AuthenticationException exception
        ) {

            /*
             * Deliberadamente devolvemos el mismo mensaje tanto
             * para email inexistente como para password incorrecto.
             *
             * Así evitamos facilitar enumeración de usuarios.
             */
            throw new BadCredentialsException(
                "Email o contraseña incorrectos."
            );
        }
    }

    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    /**
     * Renueva access token + refresh token.
     *
     * Aplicamos rotación obligatoria:
     *
     * Token A
     *   ↓
     * se utiliza
     *   ↓
     * Token A revocado
     *   ↓
     * Token B emitido
     *
     * Si Token A vuelve a utilizarse posteriormente,
     * consideramos que existe posible reutilización ilegítima
     * y revocamos todos los refresh tokens activos del usuario.
     *
     * BadCredentialsException no provoca rollback porque algunas
     * rutas de error necesitan persistir primero una revocación.
     */
    @Transactional(
        noRollbackFor = BadCredentialsException.class
    )
    public TokenResponse refresh(
        RefreshTokenRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de renovación es obligatoria."
            );
        }

        String rawRefreshToken =
            normalizeRefreshToken(
                request.refreshToken()
            );

        String tokenHash =
            hashRefreshToken(
                rawRefreshToken
            );

        RefreshToken storedToken =
            refreshTokenRepository
                .findByTokenHash(
                    tokenHash
                )
                .orElseThrow(() ->
                    new BadCredentialsException(
                        "El refresh token no es válido."
                    )
                );

        User user =
            storedToken.getUser();

        if (
            user == null
                || user.getId() == null
        ) {
            throw new BadCredentialsException(
                "El refresh token no tiene "
                    + "un usuario válido asociado."
            );
        }

        // -----------------------------------------------------
        // REUSE DETECTION
        // -----------------------------------------------------

        if (
            storedToken.isRevoked()
        ) {

            revokeAllActiveRefreshTokens(
                user.getId()
            );

            throw new BadCredentialsException(
                "El refresh token ya ha sido utilizado o revocado. "
                    + "Es necesario iniciar sesión nuevamente."
            );
        }

        // -----------------------------------------------------
        // EXPIRATION
        // -----------------------------------------------------

        if (
            storedToken.isExpired()
        ) {

            storedToken.revoke();

            refreshTokenRepository.save(
                storedToken
            );

            throw new BadCredentialsException(
                "El refresh token ha expirado. "
                    + "Es necesario iniciar sesión nuevamente."
            );
        }

        validateActiveUser(
            user
        );

        BusinessMembership membership =
            resolvePrimaryMembership(
                user.getId()
            );

        // -----------------------------------------------------
        // ROTATION
        // -----------------------------------------------------

        storedToken.revoke();

        refreshTokenRepository.save(
            storedToken
        );

        JwtService.GeneratedAccessToken newAccessToken =
            jwtService.generateAccessToken(
                user,
                membership
            );

        GeneratedRefreshToken newRefreshToken =
            issueRefreshToken(
                user
            );

        return new TokenResponse(
            newAccessToken.token(),
            newRefreshToken.rawToken(),
            TOKEN_TYPE,
            newAccessToken.expiresAt(),
            newRefreshToken.expiresAt()
        );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    /**
     * Revoca el refresh token entregado por el cliente.
     *
     * El logout es idempotente:
     *
     * si el refresh token ya no existe, consideramos
     * que la sesión ya está cerrada.
     */
    @Transactional
    public void logout(
        RefreshTokenRequest request,
        AuthenticatedUser authenticatedUser
    ) {

        if (authenticatedUser == null) {
            throw new BadCredentialsException(
                "No existe una sesión autenticada válida."
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de logout es obligatoria."
            );
        }

        String rawRefreshToken =
            normalizeRefreshToken(
                request.refreshToken()
            );

        String tokenHash =
            hashRefreshToken(
                rawRefreshToken
            );

        RefreshToken storedToken =
            refreshTokenRepository
                .findByTokenHash(
                    tokenHash
                )
                .orElse(null);

        /*
         * Logout idempotente.
         */
        if (storedToken == null) {
            return;
        }

        User tokenUser =
            storedToken.getUser();

        if (
            tokenUser == null
                || tokenUser.getId() == null
                || !tokenUser
                    .getId()
                    .equals(
                        authenticatedUser.getUserId()
                    )
        ) {
            throw new BadCredentialsException(
                "El refresh token no pertenece "
                    + "al usuario autenticado."
            );
        }

        if (
            !storedToken.isRevoked()
        ) {

            storedToken.revoke();

            refreshTokenRepository.save(
                storedToken
            );
        }
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    /**
     * Devuelve el usuario correspondiente a la sesión JWT actual.
     *
     * No confiamos únicamente en los claims del token:
     *
     * - comprobamos que el User siga existiendo
     * - comprobamos que siga activo
     * - comprobamos que la membership siga activa
     * - comprobamos que el business siga activo
     */
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(
        AuthenticatedUser authenticatedUser
    ) {

        if (authenticatedUser == null) {
            throw new BadCredentialsException(
                "No existe una sesión autenticada válida."
            );
        }

        if (
            authenticatedUser.getUserId() == null
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un userId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() == null
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un businessId válido."
            );
        }

        User user =
            userRepository
                .findById(
                    authenticatedUser.getUserId()
                )
                .orElseThrow(() ->
                    new BadCredentialsException(
                        "El usuario autenticado ya no existe."
                    )
                );

        validateActiveUser(
            user
        );

        BusinessMembership membership =
            businessMembershipRepository
                .findByBusinessIdAndUserIdAndActiveTrue(
                    authenticatedUser.getBusinessId(),
                    user.getId()
                )
                .orElseThrow(() ->
                    new BadCredentialsException(
                        "El usuario ya no tiene acceso activo "
                            + "al negocio actual."
                    )
                );

        validateMembershipForCurrentUser(
            membership
        );

        return new CurrentUserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            user.isActive(),
            user.isEmailVerified(),
            membership
                .getBusiness()
                .getId(),
            membership
                .getBusiness()
                .getName(),
            membership.getRole()
        );
    }

    // =========================================================
    // CURRENT USER · MEMBERSHIP VALIDATION
    // =========================================================

    private void validateMembershipForCurrentUser(
        BusinessMembership membership
    ) {

        if (membership == null) {
            throw new BadCredentialsException(
                "La sesión ya no tiene "
                    + "una membresía válida."
            );
        }

        if (
            !membership.isActive()
        ) {
            throw new BadCredentialsException(
                "La membresía asociada a "
                    + "la sesión está desactivada."
            );
        }

        if (
            membership.getBusiness() == null
                || membership
                    .getBusiness()
                    .getId() == null
        ) {
            throw new IllegalStateException(
                "La membresía no tiene "
                    + "un negocio válido asociado."
            );
        }

        if (
            !membership
                .getBusiness()
                .isActive()
        ) {
            throw new BadCredentialsException(
                "El negocio asociado a la sesión "
                    + "está desactivado."
            );
        }

        if (
            membership.getRole() == null
        ) {
            throw new IllegalStateException(
                "La membresía no tiene "
                    + "un rol válido."
            );
        }
    }

    // =========================================================
    // PRIMARY MEMBERSHIP
    // =========================================================

    /**
     * Resuelve la membership activa principal de un usuario.
     *
     * Actualmente seleccionamos la primera membership activa
     * ordenada por createdAt.
     *
     * Esta estrategia permite posteriormente evolucionar
     * hacia selección explícita de workspace/business.
     */
    private BusinessMembership resolvePrimaryMembership(
        Long userId
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                "userId es obligatorio."
            );
        }

        List<BusinessMembership> memberships =
            businessMembershipRepository
                .findAllByUserIdAndActiveTrueOrderByCreatedAtAsc(
                    userId
                );

        if (
            memberships == null
                || memberships.isEmpty()
        ) {
            throw new BadCredentialsException(
                "El usuario no tiene acceso activo "
                    + "a ningún negocio."
            );
        }

        BusinessMembership membership =
            memberships.getFirst();

        if (
            membership.getBusiness() == null
                || membership
                    .getBusiness()
                    .getId() == null
        ) {
            throw new IllegalStateException(
                "La membresía principal no tiene "
                    + "un negocio válido asociado."
            );
        }

        if (
            !membership
                .getBusiness()
                .isActive()
        ) {
            throw new BadCredentialsException(
                "El negocio asociado al usuario "
                    + "está desactivado."
            );
        }

        if (
            membership.getRole() == null
        ) {
            throw new IllegalStateException(
                "La membresía principal no tiene "
                    + "un rol válido."
            );
        }

        return membership;
    }

    // =========================================================
    // REFRESH TOKEN CREATION
    // =========================================================

    private GeneratedRefreshToken issueRefreshToken(
        User user
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                "El usuario es obligatorio para emitir "
                    + "un refresh token."
            );
        }

        if (user.getId() == null) {
            throw new IllegalStateException(
                "No se puede emitir un refresh token "
                    + "para un usuario no persistido."
            );
        }

        if (
            refreshTokenDays <= 0
        ) {
            throw new IllegalStateException(
                "app.security.jwt.refresh-token-days "
                    + "debe ser mayor que 0."
            );
        }

        String rawToken =
            generateSecureRefreshToken();

        String tokenHash =
            hashRefreshToken(
                rawToken
            );

        OffsetDateTime expiresAt =
            OffsetDateTime
                .now()
                .plusDays(
                    refreshTokenDays
                );

        RefreshToken entity =
            RefreshToken
                .builder()
                .user(
                    user
                )
                .tokenHash(
                    tokenHash
                )
                .expiresAt(
                    expiresAt
                )
                .build();

        refreshTokenRepository.save(
            entity
        );

        return new GeneratedRefreshToken(
            rawToken,
            expiresAt
        );
    }

    // =========================================================
    // REFRESH TOKEN · REVOKE ALL
    // =========================================================

    private void revokeAllActiveRefreshTokens(
        Long userId
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                "userId es obligatorio."
            );
        }

        refreshTokenRepository
            .revokeAllActiveByUserId(
                userId,
                OffsetDateTime.now()
            );
    }

    // =========================================================
    // REFRESH TOKEN RANDOM GENERATION
    // =========================================================

    /**
     * Genera 64 bytes criptográficamente aleatorios.
     *
     * El resultado se codifica como Base64 URL-safe
     * sin padding.
     */
    private String generateSecureRefreshToken() {

        byte[] randomBytes =
            new byte[
                REFRESH_TOKEN_RANDOM_BYTES
            ];

        SECURE_RANDOM.nextBytes(
            randomBytes
        );

        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                randomBytes
            );
    }

    // =========================================================
    // REFRESH TOKEN HASHING
    // =========================================================

    /**
     * PostgreSQL nunca almacena el refresh token original.
     *
     * Únicamente almacenamos SHA-256 del token.
     */
    private String hashRefreshToken(
        String rawToken
    ) {

        if (
            rawToken == null
                || rawToken.isBlank()
        ) {
            throw new IllegalArgumentException(
                "rawToken es obligatorio."
            );
        }

        try {

            MessageDigest digest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            byte[] hash =
                digest.digest(
                    rawToken.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    hash
                );

        } catch (
            NoSuchAlgorithmException exception
        ) {

            /*
             * SHA-256 forma parte obligatoria de la JVM.
             *
             * Si esto falla existe un problema grave
             * con el runtime.
             */
            throw new IllegalStateException(
                "SHA-256 no está disponible en la JVM.",
                exception
            );
        }
    }

    // =========================================================
    // USER VALIDATION
    // =========================================================

    private void validateActiveUser(
        User user
    ) {

        if (user == null) {
            throw new BadCredentialsException(
                "El usuario no existe."
            );
        }

        if (
            user.getId() == null
        ) {
            throw new IllegalStateException(
                "El usuario no está persistido."
            );
        }

        if (
            !user.isActive()
        ) {
            throw new BadCredentialsException(
                "La cuenta de usuario está desactivada."
            );
        }
    }

    // =========================================================
    // EMAIL NORMALIZATION
    // =========================================================

    private String normalizeEmail(
        String email
    ) {

        if (
            email == null
                || email.isBlank()
        ) {
            throw new BadCredentialsException(
                "Email o contraseña incorrectos."
            );
        }

        return email
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }

    // =========================================================
    // REFRESH TOKEN NORMALIZATION
    // =========================================================

    private String normalizeRefreshToken(
        String refreshToken
    ) {

        if (
            refreshToken == null
                || refreshToken.isBlank()
        ) {
            throw new BadCredentialsException(
                "El refresh token es obligatorio."
            );
        }

        return refreshToken.trim();
    }

    // =========================================================
    // INTERNAL RESULT
    // =========================================================

    private record GeneratedRefreshToken(

        String rawToken,

        OffsetDateTime expiresAt

    ) {
    }
}