package com.wacds.appointments.professional.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record ProfessionalRequest(

    // =========================================================
    // FIRST NAME
    // =========================================================

    @NotBlank(
        message = "El nombre del profesional es obligatorio."
    )
    @Size(
        max = 100,
        message = "El nombre del profesional no puede superar los 100 caracteres."
    )
    String firstName,

    // =========================================================
    // LAST NAME
    // =========================================================

    @Size(
        max = 150,
        message = "Los apellidos no pueden superar los 150 caracteres."
    )
    String lastName,

    // =========================================================
    // DISPLAY NAME
    // =========================================================

    @Size(
        max = 180,
        message = "El nombre visible no puede superar los 180 caracteres."
    )
    String displayName,

    // =========================================================
    // EMAIL
    // =========================================================

    @Email(
        message = "El email del profesional no tiene un formato válido."
    )
    @Size(
        max = 180,
        message = "El email no puede superar los 180 caracteres."
    )
    String email,

    // =========================================================
    // PHONE
    // =========================================================

    @Size(
        max = 40,
        message = "El teléfono no puede superar los 40 caracteres."
    )
    String phone,

    // =========================================================
    // BIO
    // =========================================================

    @Size(
        max = 5000,
        message = "La biografía no puede superar los 5000 caracteres."
    )
    String bio,

    // =========================================================
    // ACTIVE
    // =========================================================

    /**
     * Indica si el profesional forma parte actualmente
     * del equipo operativo del negocio.
     *
     * null se normaliza a true para mantener compatibilidad
     * con peticiones anteriores.
     */
    Boolean active,

    // =========================================================
    // BOOKING ENABLED
    // =========================================================

    /**
     * Indica si el profesional puede recibir nuevas reservas.
     *
     * Puede existir:
     *
     * active = true
     * bookingEnabled = false
     *
     * por ejemplo durante vacaciones, bajas o periodos
     * temporalmente fuera de agenda.
     *
     * null se normaliza a true para mantener compatibilidad.
     */
    Boolean bookingEnabled

) {

    // =========================================================
    // COMPACT CONSTRUCTOR
    // =========================================================

    public ProfessionalRequest {

        // -----------------------------------------------------
        // FIRST NAME
        // -----------------------------------------------------

        if (
            firstName != null
        ) {
            firstName =
                firstName.trim();
        }

        // -----------------------------------------------------
        // LAST NAME
        // -----------------------------------------------------

        lastName =
            normalizeNullableText(
                lastName
            );

        // -----------------------------------------------------
        // DISPLAY NAME
        // -----------------------------------------------------

        displayName =
            normalizeNullableText(
                displayName
            );

        // -----------------------------------------------------
        // EMAIL
        // -----------------------------------------------------

        if (
            email != null
        ) {
            email =
                email.trim();

            if (
                email.isEmpty()
            ) {
                email =
                    null;
            } else {
                email =
                    email.toLowerCase(
                        Locale.ROOT
                    );
            }
        }

        // -----------------------------------------------------
        // PHONE
        // -----------------------------------------------------

        phone =
            normalizeNullableText(
                phone
            );

        // -----------------------------------------------------
        // BIO
        // -----------------------------------------------------

        bio =
            normalizeNullableText(
                bio
            );

        // -----------------------------------------------------
        // ACTIVE
        // -----------------------------------------------------

        if (
            active == null
        ) {
            active =
                true;
        }

        // -----------------------------------------------------
        // BOOKING ENABLED
        // -----------------------------------------------------

        if (
            bookingEnabled == null
        ) {
            bookingEnabled =
                true;
        }

        /*
         * Coherencia de dominio:
         *
         * un profesional inactivo no puede permanecer
         * habilitado para recibir reservas.
         */
        if (
            Boolean.FALSE.equals(
                active
            )
        ) {
            bookingEnabled =
                false;
        }
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private static String normalizeNullableText(
        String value
    ) {

        if (
            value == null
                || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}