package com.wacds.appointments.customer.dto;

import java.time.OffsetDateTime;

/**
 * Respuesta pública interna utilizada por el panel Business
 * para representar clientes del negocio.
 *
 * Incluye:
 *
 * - identificación del cliente;
 * - tenant al que pertenece;
 * - datos personales y de contacto;
 * - nombre completo calculado;
 * - notas internas;
 * - estado activo;
 * - fechas de auditoría.
 */
public record CustomerResponse(

    // =========================================================
    // IDENTITY
    // =========================================================

    Long id,

    Long businessId,

    // =========================================================
    // CUSTOMER DATA
    // =========================================================

    String firstName,

    String lastName,

    String fullName,

    String email,

    String phone,

    String notes,

    // =========================================================
    // STATUS
    // =========================================================

    boolean active,

    // =========================================================
    // AUDIT
    // =========================================================

    OffsetDateTime createdAt,

    OffsetDateTime updatedAt

) {
}