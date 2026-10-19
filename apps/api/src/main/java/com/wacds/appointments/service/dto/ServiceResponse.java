package com.wacds.appointments.service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Respuesta utilizada por la API Business para representar
 * un servicio perteneciente a un negocio.
 *
 * Incluye:
 *
 * - identificación;
 * - negocio propietario;
 * - información comercial;
 * - duración;
 * - precio;
 * - estado operativo;
 * - disponibilidad para reservas;
 * - auditoría.
 *
 * Este DTO puede utilizarse tanto en listados administrativos
 * como en selectores internos del panel Business.
 */
public record ServiceResponse(

    // =========================================================
    // IDENTITY
    // =========================================================

    Long id,

    Long businessId,

    // =========================================================
    // SERVICE DATA
    // =========================================================

    String name,

    String slug,

    String description,

    // =========================================================
    // BOOKING CONFIGURATION
    // =========================================================

    Integer durationMinutes,

    BigDecimal price,

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