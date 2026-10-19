package com.wacds.appointments.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Locale;

public record ServiceRequest(

    // =========================================================
    // NAME
    // =========================================================

    @NotBlank(
        message = "El nombre del servicio es obligatorio."
    )
    @Size(
        max = 150,
        message = "El nombre del servicio no puede superar los 150 caracteres."
    )
    String name,

    // =========================================================
    // SLUG
    // =========================================================

    @NotBlank(
        message = "El slug del servicio es obligatorio."
    )
    @Size(
        max = 180,
        message = "El slug del servicio no puede superar los 180 caracteres."
    )
    String slug,

    // =========================================================
    // DESCRIPTION
    // =========================================================

    @Size(
        max = 5000,
        message = "La descripción no puede superar los 5000 caracteres."
    )
    String description,

    // =========================================================
    // DURATION
    // =========================================================

    @NotNull(
        message = "La duración del servicio es obligatoria."
    )
    @Min(
        value = 1,
        message = "La duración del servicio debe ser de al menos 1 minuto."
    )
    Integer durationMinutes,

    // =========================================================
    // PRICE
    // =========================================================

    @NotNull(
        message = "El precio del servicio es obligatorio."
    )
    @DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "El precio del servicio no puede ser negativo."
    )
    BigDecimal price,

    // =========================================================
    // ACTIVE
    // =========================================================

    /**
     * Indica si el servicio forma parte actualmente
     * del catálogo operativo del negocio.
     *
     * null se normaliza a true para conservar compatibilidad
     * con consumidores anteriores del endpoint.
     */
    Boolean active,

    // =========================================================
    // BOOKING ENABLED
    // =========================================================

    /**
     * Indica si el servicio admite nuevas reservas.
     *
     * Un servicio puede permanecer activo en el catálogo
     * aunque temporalmente no acepte reservas.
     *
     * null se normaliza a true para conservar compatibilidad.
     */
    Boolean bookingEnabled

) {

    // =========================================================
    // COMPACT CONSTRUCTOR
    // =========================================================

    public ServiceRequest {

        // -----------------------------------------------------
        // NAME
        // -----------------------------------------------------

        if (
            name != null
        ) {
            name =
                name.trim();
        }

        // -----------------------------------------------------
        // SLUG
        // -----------------------------------------------------

        if (
            slug != null
        ) {
            slug =
                slug
                    .trim()
                    .toLowerCase(
                        Locale.ROOT
                    );
        }

        // -----------------------------------------------------
        // DESCRIPTION
        // -----------------------------------------------------

        if (
            description != null
        ) {
            description =
                description.trim();

            if (
                description.isEmpty()
            ) {
                description =
                    null;
            }
        }

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
         * un servicio inactivo no puede aceptar reservas.
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
}