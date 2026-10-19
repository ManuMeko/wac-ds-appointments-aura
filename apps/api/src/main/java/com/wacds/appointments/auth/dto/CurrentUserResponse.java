package com.wacds.appointments.auth.dto;

import com.wacds.appointments.auth.membership.BusinessRole;

public record CurrentUserResponse(

    Long userId,

    String email,

    String firstName,

    String lastName,

    String fullName,

    boolean active,

    boolean emailVerified,

    Long businessId,

    String businessName,

    BusinessRole role

) {
}