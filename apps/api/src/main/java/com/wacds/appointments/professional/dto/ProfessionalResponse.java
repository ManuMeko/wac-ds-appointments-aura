package com.wacds.appointments.professional.dto;

import java.time.OffsetDateTime;

/**
 * Respuesta utilizada por la API Business para representar
 * profesionales del negocio.
 *
 * Incluye:
 *
 * - identificación;
 * - negocio al que pertenece;
 * - datos personales y de contacto;
 * - nombre visible;
 * - biografía;
 * - estado operativo;
 * - disponibilidad para reservas;
 * - auditoría.
 */
public record ProfessionalResponse(

    // =========================================================
    // IDENTITY
    // =========================================================

    Long id,

    Long businessId,

    // =========================================================
    // PROFESSIONAL DATA
    // =========================================================

    String firstName,

    String lastName,

    String displayName,

    String email,

    String phone,

    String bio,

    // =========================================================
    // STATUS
    // =========================================================

    boolean active,

    boolean bookingEnabled,

    // =========================================================
    // AUDIT
    // =========================================================

    OffsetDateTime createdAt,

    OffsetDateTime updatedAt

) {
}