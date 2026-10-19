package com.wacds.appointments.auth.membership.dto;

import jakarta.validation.constraints.NotNull;

public record BusinessMembershipStatusUpdateRequest(

    @NotNull(
        message = "El estado active es obligatorio."
    )
    Boolean active

) {
}