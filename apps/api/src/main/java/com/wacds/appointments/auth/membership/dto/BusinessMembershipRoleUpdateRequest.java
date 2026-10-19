package com.wacds.appointments.auth.membership.dto;

import com.wacds.appointments.auth.membership.BusinessRole;
import jakarta.validation.constraints.NotNull;

public record BusinessMembershipRoleUpdateRequest(

    @NotNull(
        message = "El rol es obligatorio."
    )
    BusinessRole role

) {
}