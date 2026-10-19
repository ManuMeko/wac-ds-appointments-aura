package com.wacds.appointments.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(

    @NotBlank(
        message = "El refresh token es obligatorio."
    )
    @Size(
        max = 2048,
        message = "El refresh token no puede superar los 2048 caracteres."
    )
    String refreshToken

) {
}