package com.wacds.appointments.auth.token;

import com.wacds.appointments.auth.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(
            name = "ix_refresh_tokens_user",
            columnList = "user_id"
        ),
        @Index(
            name = "ix_refresh_tokens_expires_at",
            columnList = "expires_at"
        ),
        @Index(
            name = "ix_refresh_tokens_revoked_at",
            columnList = "revoked_at"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    // =========================================================
    // USER
    // =========================================================

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_refresh_tokens_user"
        )
    )
    private User user;

    // =========================================================
    // TOKEN
    // =========================================================

    /**
     * SHA-256 (u otro hash criptográfico equivalente)
     * del refresh token real.
     *
     * El token en claro nunca debe persistirse.
     */
    @Column(
        name = "token_hash",
        nullable = false,
        length = 255
    )
    private String tokenHash;

    // =========================================================
    // EXPIRATION
    // =========================================================

    @Column(
        name = "expires_at",
        nullable = false
    )
    private OffsetDateTime expiresAt;

    // =========================================================
    // REVOCATION
    // =========================================================

    @Column(
        name = "revoked_at"
    )
    private OffsetDateTime revokedAt;

    // =========================================================
    // AUDIT
    // =========================================================

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt =
                OffsetDateTime.now();
        }

        if (user == null) {
            throw new IllegalStateException(
                "El usuario del refresh token es obligatorio."
            );
        }

        if (
            tokenHash == null
                || tokenHash.isBlank()
        ) {
            throw new IllegalStateException(
                "El hash del refresh token es obligatorio."
            );
        }

        if (expiresAt == null) {
            throw new IllegalStateException(
                "La fecha de expiración del refresh token es obligatoria."
            );
        }
    }

    // =========================================================
    // DOMAIN HELPERS
    // =========================================================

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(
            OffsetDateTime.now()
        );
    }

    public boolean isValid() {
        return !isRevoked()
            && !isExpired();
    }

    public void revoke() {

        if (revokedAt == null) {
            revokedAt =
                OffsetDateTime.now();
        }
    }
}