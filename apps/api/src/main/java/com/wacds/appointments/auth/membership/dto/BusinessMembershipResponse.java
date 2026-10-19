package com.wacds.appointments.auth.membership.dto;

import com.wacds.appointments.auth.membership.BusinessRole;

import java.time.OffsetDateTime;

public record BusinessMembershipResponse(

    Long id,

    Long businessId,

    Long userId,

    String email,

    String firstName,

    String lastName,

    String fullName,

    BusinessRole role,

    boolean active,

    boolean userActive,

    boolean emailVerified,

    OffsetDateTime lastLoginAt,

    OffsetDateTime createdAt,

    OffsetDateTime updatedAt

) {
}