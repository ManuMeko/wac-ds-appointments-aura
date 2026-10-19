package com.wacds.appointments.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record BusinessUpdateRequest(

    // =========================================================
    // IDENTITY
    // =========================================================

    @Size(
        max = 150,
        message = "El nombre no puede superar los 150 caracteres."
    )
    String name,

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
    @Pattern(
        regexp = "^[A-Za-z]{2}$",
        message = "El código de país debe contener únicamente 2 letras."
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
    @Pattern(
        regexp = "^[A-Za-z]{3}$",
        message = "La moneda debe contener únicamente 3 letras."
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

    /**
     * Habilita o deshabilita el sistema de reservas públicas
     * del negocio.
     *
     * null:
     * no modifica el valor actual.
     *
     * true:
     * habilita reservas públicas.
     *
     * false:
     * deshabilita reservas públicas.
     */
    Boolean publicBookingEnabled

) {

    // =========================================================
    // COMPACT CONSTRUCTOR
    // =========================================================

    /**
     * Normalización previa a la capa de servicio.
     *
     * IMPORTANTE:
     *
     * Este DTO se utiliza para PATCH.
     *
     * Por tanto:
     *
     * null = el campo no se ha enviado y no debe modificarse.
     *
     * No convertimos automáticamente cadenas vacías a null
     * porque hacerlo eliminaría la capacidad de distinguir
     * entre:
     *
     * - campo no enviado;
     * - campo enviado vacío.
     */
    public BusinessUpdateRequest {

        // -----------------------------------------------------
        // IDENTITY
        // -----------------------------------------------------

        name =
            trimPreservingNull(
                name
            );

        slug =
            normalizeSlug(
                slug
            );

        description =
            trimPreservingNull(
                description
            );

        // -----------------------------------------------------
        // CONTACT
        // -----------------------------------------------------

        email =
            normalizeEmail(
                email
            );

        phone =
            trimPreservingNull(
                phone
            );

        websiteUrl =
            trimPreservingNull(
                websiteUrl
            );

        // -----------------------------------------------------
        // ADDRESS
        // -----------------------------------------------------

        addressLine1 =
            trimPreservingNull(
                addressLine1
            );

        addressLine2 =
            trimPreservingNull(
                addressLine2
            );

        postalCode =
            trimPreservingNull(
                postalCode
            );

        city =
            trimPreservingNull(
                city
            );

        stateRegion =
            trimPreservingNull(
                stateRegion
            );

        countryCode =
            normalizeUppercase(
                countryCode
            );

        // -----------------------------------------------------
        // REGIONAL SETTINGS
        // -----------------------------------------------------

        timezone =
            trimPreservingNull(
                timezone
            );

        locale =
            trimPreservingNull(
                locale
            );

        currency =
            normalizeUppercase(
                currency
            );

        // -----------------------------------------------------
        // BRANDING
        // -----------------------------------------------------

        logoUrl =
            trimPreservingNull(
                logoUrl
            );

        coverImageUrl =
            trimPreservingNull(
                coverImageUrl
            );

        primaryColor =
            normalizeHexColor(
                primaryColor
            );

        secondaryColor =
            normalizeHexColor(
                secondaryColor
            );
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private static String trimPreservingNull(
        String value
    ) {

        if (
            value == null
        ) {
            return null;
        }

        return value.trim();
    }

    private static String normalizeSlug(
        String value
    ) {

        if (
            value == null
        ) {
            return null;
        }

        return value
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }

    private static String normalizeEmail(
        String value
    ) {

        if (
            value == null
        ) {
            return null;
        }

        return value
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }

    private static String normalizeUppercase(
        String value
    ) {

        if (
            value == null
        ) {
            return null;
        }

        return value
            .trim()
            .toUpperCase(
                Locale.ROOT
            );
    }

    private static String normalizeHexColor(
        String value
    ) {

        if (
            value == null
        ) {
            return null;
        }

        return value
            .trim()
            .toUpperCase(
                Locale.ROOT
            );
    }
}