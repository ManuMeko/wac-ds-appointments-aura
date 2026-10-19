package com.wacds.appointments.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessRequest(

    // =========================================================
    // IDENTITY
    // =========================================================

    @NotBlank(
        message = "El nombre del negocio es obligatorio."
    )
    @Size(
        max = 150,
        message = "El nombre no puede superar los 150 caracteres."
    )
    String name,

    @NotBlank(
        message = "El slug del negocio es obligatorio."
    )
    @Size(
        max = 180,
        message = "El slug no puede superar los 180 caracteres."
    )
    @Pattern(
        regexp = "^[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*$",
        message = "El slug solo puede contener letras, números y guiones."
    )
    String slug,

    @Size(
        max = 5000,
        message = "La descripción no puede superar los 5000 caracteres."
    )
    String description,

    // =========================================================
    // CONTACT
    // =========================================================

    @NotBlank(
        message = "El email del negocio es obligatorio."
    )
    @Email(
        message = "El email no tiene un formato válido."
    )
    @Size(
        max = 180,
        message = "El email no puede superar los 180 caracteres."
    )
    String email,

    @Size(
        max = 40,
        message = "El teléfono no puede superar los 40 caracteres."
    )
    String phone,

    @Size(
        max = 500,
        message = "La URL web no puede superar los 500 caracteres."
    )
    String websiteUrl,

    // =========================================================
    // ADDRESS
    // =========================================================

    @Size(
        max = 180,
        message = "La dirección principal no puede superar los 180 caracteres."
    )
    String addressLine1,

    @Size(
        max = 180,
        message = "La dirección secundaria no puede superar los 180 caracteres."
    )
    String addressLine2,

    @Size(
        max = 30,
        message = "El código postal no puede superar los 30 caracteres."
    )
    String postalCode,

    @Size(
        max = 120,
        message = "La ciudad no puede superar los 120 caracteres."
    )
    String city,

    @Size(
        max = 120,
        message = "La provincia o región no puede superar los 120 caracteres."
    )
    String stateRegion,

    @Size(
        min = 2,
        max = 2,
        message = "El código de país debe contener exactamente 2 caracteres."
    )
    String countryCode,

    // =========================================================
    // REGIONAL SETTINGS
    // =========================================================

    @Size(
        max = 80,
        message = "La zona horaria no puede superar los 80 caracteres."
    )
    String timezone,

    @Size(
        max = 20,
        message = "El locale no puede superar los 20 caracteres."
    )
    String locale,

    @Size(
        min = 3,
        max = 3,
        message = "La moneda debe contener exactamente 3 caracteres."
    )
    String currency,

    // =========================================================
    // BRANDING
    // =========================================================

    @Size(
        max = 500,
        message = "La URL del logo no puede superar los 500 caracteres."
    )
    String logoUrl,

    @Size(
        max = 500,
        message = "La URL de portada no puede superar los 500 caracteres."
    )
    String coverImageUrl,

    @Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color principal debe utilizar formato hexadecimal #RRGGBB."
    )
    String primaryColor,

    @Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color secundario debe utilizar formato hexadecimal #RRGGBB."
    )
    String secondaryColor,

    // =========================================================
    // PUBLIC BOOKING
    // =========================================================

    Boolean publicBookingEnabled

) {
}