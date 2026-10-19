package com.wacds.appointments.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank(
        message = "El email es obligatorio."
    )
    @Email(
        message = "El formato del email no es válido."
    )
    @Size(
        max = 255,
        message = "El email no puede superar los 255 caracteres."
    )
    String email,

    @NotBlank(
        message = "La contraseña es obligatoria."
    )
    @Size(
        min = 8,
        max = 128,
        message = "La contraseña debe tener entre 8 y 128 caracteres."
    )
    String password

) {
}