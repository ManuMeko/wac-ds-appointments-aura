package com.wacds.appointments.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository
    extends JpaRepository<RefreshToken, Long> {

    // =========================================================
    // FIND BY TOKEN HASH
    // =========================================================

    /**
     * Busca un refresh token por su hash SHA-256.
     *
     * Incluye tokens:
     *
     * - activos
     * - revocados
     * - expirados
     *
     * Es necesario para poder detectar intentos de reutilización
     * de refresh tokens previamente revocados.
     */
    Optional<RefreshToken> findByTokenHash(
        String tokenHash
    );

    /**
     * Busca únicamente un refresh token que todavía
     * no haya sido revocado.
     */
    Optional<RefreshToken>
        findByTokenHashAndRevokedAtIsNull(
            String tokenHash
        );

    // =========================================================
    // USER TOKENS
    // =========================================================

    /**
     * Obtiene todos los refresh tokens de un usuario,
     * incluyendo activos, revocados y expirados.
     */
    List<RefreshToken>
        findAllByUserIdOrderByCreatedAtDesc(
            Long userId
        );

    /**
     * Obtiene todos los refresh tokens no revocados
     * pertenecientes a un usuario.
     */
    List<RefreshToken>
        findAllByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(
            Long userId
        );

    // =========================================================
    // EXISTS
    // =========================================================

    /**
     * Comprueba si ya existe un determinado hash.
     *
     * Aunque la probabilidad de colisión de los tokens
     * aleatorios es extremadamente baja, este método nos
     * permite disponer de esa comprobación cuando sea necesaria.
     */
    boolean existsByTokenHash(
        String tokenHash
    );

    // =========================================================
    // REVOKE ALL ACTIVE TOKENS
    // =========================================================

    /**
     * Revoca todos los refresh tokens actualmente activos
     * pertenecientes a un usuario.
     *
     * Se utiliza, entre otros casos, cuando detectamos la
     * reutilización de un refresh token previamente revocado.
     *
     * Devuelve el número de registros modificados.
     *
     * Este método se ejecuta dentro de una transacción iniciada
     * desde la capa de servicio.
     */
    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query("""
        UPDATE RefreshToken refreshToken
        SET refreshToken.revokedAt = :revokedAt
        WHERE refreshToken.user.id = :userId
          AND refreshToken.revokedAt IS NULL
        """)
    int revokeAllActiveByUserId(
        @Param("userId")
        Long userId,

        @Param("revokedAt")
        OffsetDateTime revokedAt
    );

    // =========================================================
    // CLEANUP
    // =========================================================

    /**
     * Elimina refresh tokens cuya fecha de expiración sea
     * anterior al umbral indicado.
     *
     * Más adelante podremos ejecutar esta operación mediante
     * una tarea programada de mantenimiento.
     *
     * Devuelve el número de registros eliminados.
     */
    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query("""
        DELETE FROM RefreshToken refreshToken
        WHERE refreshToken.expiresAt < :threshold
        """)
    int deleteExpiredTokensBefore(
        @Param("threshold")
        OffsetDateTime threshold
    );
}