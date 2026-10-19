package com.wacds.appointments.auth.security;

import com.wacds.appointments.auth.membership.BusinessMembership;
import com.wacds.appointments.auth.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class JwtService {

    // =========================================================
    // CLAIM NAMES
    // =========================================================

    public static final String CLAIM_USER_ID =
        "userId";

    public static final String CLAIM_EMAIL =
        "email";

    public static final String CLAIM_BUSINESS_ID =
        "businessId";

    public static final String CLAIM_ROLE =
        "role";

    public static final String CLAIM_TOKEN_TYPE =
        "tokenType";

    public static final String TOKEN_TYPE_ACCESS =
        "ACCESS";

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final JwtEncoder jwtEncoder;

    private final JwtDecoder jwtDecoder;

    // =========================================================
    // CONFIGURATION
    // =========================================================

    @Value(
        "${app.security.jwt.issuer:wac-ds-appointments}"
    )
    private String issuer;

    @Value(
        "${app.security.jwt.access-token-minutes:15}"
    )
    private long accessTokenMinutes;

    // =========================================================
    // TOKEN GENERATION
    // =========================================================

    public GeneratedAccessToken generateAccessToken(
        User user,
        BusinessMembership membership
    ) {
        validateTokenContext(
            user,
            membership
        );

        Instant issuedAt =
            Instant.now();

        Instant expiresAt =
            issuedAt.plus(
                Duration.ofMinutes(
                    accessTokenMinutes
                )
            );

        JwtClaimsSet claims =
            JwtClaimsSet
                .builder()
                .issuer(
                    issuer
                )
                .subject(
                    user
                        .getId()
                        .toString()
                )
                .issuedAt(
                    issuedAt
                )
                .expiresAt(
                    expiresAt
                )
                .claim(
                    CLAIM_USER_ID,
                    user.getId()
                )
                .claim(
                    CLAIM_EMAIL,
                    user.getEmail()
                )
                .claim(
                    CLAIM_BUSINESS_ID,
                    membership
                        .getBusiness()
                        .getId()
                )
                .claim(
                    CLAIM_ROLE,
                    membership
                        .getRole()
                        .name()
                )
                .claim(
                    CLAIM_TOKEN_TYPE,
                    TOKEN_TYPE_ACCESS
                )
                .build();

        String token =
            jwtEncoder
                .encode(
                    JwtEncoderParameters.from(
                        claims
                    )
                )
                .getTokenValue();

        OffsetDateTime expiration =
            OffsetDateTime.ofInstant(
                expiresAt,
                ZoneOffset.UTC
            );

        return new GeneratedAccessToken(
            token,
            expiration
        );
    }

    // =========================================================
    // TOKEN DECODING
    // =========================================================

    public Jwt decode(
        String token
    ) {
        if (
            token == null
                || token.isBlank()
        ) {
            throw new IllegalArgumentException(
                "El access token es obligatorio."
            );
        }

        return jwtDecoder.decode(
            token.trim()
        );
    }

    // =========================================================
    // CLAIM EXTRACTION
    // =========================================================

    public Long extractUserId(
        Jwt jwt
    ) {
        Objects.requireNonNull(
            jwt,
            "jwt no puede ser null"
        );

        Number value =
            jwt.getClaim(
                CLAIM_USER_ID
            );

        if (value == null) {
            throw new IllegalArgumentException(
                "El JWT no contiene userId."
            );
        }

        return value.longValue();
    }

    public String extractEmail(
        Jwt jwt
    ) {
        Objects.requireNonNull(
            jwt,
            "jwt no puede ser null"
        );

        String value =
            jwt.getClaimAsString(
                CLAIM_EMAIL
            );

        if (
            value == null
                || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                "El JWT no contiene email."
            );
        }

        return value;
    }

    public Long extractBusinessId(
        Jwt jwt
    ) {
        Objects.requireNonNull(
            jwt,
            "jwt no puede ser null"
        );

        Number value =
            jwt.getClaim(
                CLAIM_BUSINESS_ID
            );

        if (value == null) {
            throw new IllegalArgumentException(
                "El JWT no contiene businessId."
            );
        }

        return value.longValue();
    }

    public String extractRole(
        Jwt jwt
    ) {
        Objects.requireNonNull(
            jwt,
            "jwt no puede ser null"
        );

        String value =
            jwt.getClaimAsString(
                CLAIM_ROLE
            );

        if (
            value == null
                || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                "El JWT no contiene role."
            );
        }

        return value;
    }

    public String extractTokenType(
        Jwt jwt
    ) {
        Objects.requireNonNull(
            jwt,
            "jwt no puede ser null"
        );

        String value =
            jwt.getClaimAsString(
                CLAIM_TOKEN_TYPE
            );

        if (
            value == null
                || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                "El JWT no contiene tokenType."
            );
        }

        return value;
    }

    // =========================================================
    // TOKEN VALIDATION
    // =========================================================

    public boolean isAccessToken(
        Jwt jwt
    ) {
        if (jwt == null) {
            return false;
        }

        String tokenType =
            jwt.getClaimAsString(
                CLAIM_TOKEN_TYPE
            );

        return TOKEN_TYPE_ACCESS.equals(
            tokenType
        );
    }

    public boolean isExpired(
        Jwt jwt
    ) {
        if (
            jwt == null
                || jwt.getExpiresAt() == null
        ) {
            return true;
        }

        return jwt
            .getExpiresAt()
            .isBefore(
                Instant.now()
            );
    }

    // =========================================================
    // CONTEXT VALIDATION
    // =========================================================

    private void validateTokenContext(
        User user,
        BusinessMembership membership
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                "El usuario es obligatorio para generar el JWT."
            );
        }

        if (user.getId() == null) {
            throw new IllegalArgumentException(
                "El usuario debe estar persistido antes de generar el JWT."
            );
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException(
                "No se puede generar un JWT para un usuario inactivo."
            );
        }

        if (
            user.getEmail() == null
                || user.getEmail().isBlank()
        ) {
            throw new IllegalArgumentException(
                "El usuario debe tener un email válido."
            );
        }

        if (membership == null) {
            throw new IllegalArgumentException(
                "La membresía del negocio es obligatoria."
            );
        }

        if (!membership.isActive()) {
            throw new IllegalArgumentException(
                "No se puede generar un JWT para una membresía inactiva."
            );
        }

        if (membership.getBusiness() == null) {
            throw new IllegalArgumentException(
                "La membresía no tiene un negocio asociado."
            );
        }

        if (membership.getBusiness().getId() == null) {
            throw new IllegalArgumentException(
                "El negocio de la membresía debe estar persistido."
            );
        }

        if (membership.getUser() == null) {
            throw new IllegalArgumentException(
                "La membresía no tiene un usuario asociado."
            );
        }

        if (
            membership.getUser().getId() == null
                || !membership
                    .getUser()
                    .getId()
                    .equals(
                        user.getId()
                    )
        ) {
            throw new IllegalArgumentException(
                "La membresía no pertenece al usuario indicado."
            );
        }

        if (membership.getRole() == null) {
            throw new IllegalArgumentException(
                "La membresía debe tener un rol."
            );
        }
    }

    // =========================================================
    // INTERNAL RESULT
    // =========================================================

    public record GeneratedAccessToken(

        String token,

        OffsetDateTime expiresAt

    ) {
    }
}