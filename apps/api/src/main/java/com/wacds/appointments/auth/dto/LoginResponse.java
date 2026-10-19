package com.wacds.appointments.auth.dto;

import com.wacds.appointments.auth.membership.BusinessRole;

import java.time.OffsetDateTime;

public record LoginResponse(

    String accessToken,

    String refreshToken,

    String tokenType,

    OffsetDateTime accessTokenExpiresAt,

    OffsetDateTime refreshTokenExpiresAt,

    Long userId,

    String email,

    String firstName,

    String lastName,

    String fullName,

    Long businessId,

    String businessName,

    BusinessRole role

) {
}