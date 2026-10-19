package com.wacds.appointments.business.dto;

import java.time.OffsetDateTime;

public record BusinessResponse(

    // =========================================================
    // IDENTITY
    // =========================================================

    Long id,

    String name,

    String slug,

    String description,

    // =========================================================
    // CONTACT
    // =========================================================

    String email,

    String phone,

    String websiteUrl,

    // =========================================================
    // ADDRESS
    // =========================================================

    String addressLine1,

    String addressLine2,

    String postalCode,

    String city,

    String stateRegion,

    String countryCode,

    // =========================================================
    // REGIONAL SETTINGS
    // =========================================================

    String timezone,

    String locale,

    String currency,

    // =========================================================
    // BRANDING
    // =========================================================

    String logoUrl,

    String coverImageUrl,

    String primaryColor,

    String secondaryColor,

    // =========================================================
    // PUBLIC BOOKING
    // =========================================================

    /**
     * Indica si el negocio permite actualmente
     * reservas públicas.
     */
    boolean publicBookingEnabled,

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Estado administrativo general del Business.
     *
     * Se expone para consulta, pero no debe modificarse
     * desde la pantalla normal de configuración.
     */
    boolean active,

    // =========================================================
    // AUDIT
    // =========================================================

    OffsetDateTime createdAt,

    OffsetDateTime updatedAt

) {
}