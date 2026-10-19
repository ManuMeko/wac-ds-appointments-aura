package com.wacds.appointments.auth.dto;

import java.time.OffsetDateTime;

public record TokenResponse(

    String accessToken,

    String refreshToken,

    String tokenType,

    OffsetDateTime accessTokenExpiresAt,

    OffsetDateTime refreshTokenExpiresAt

) {
}