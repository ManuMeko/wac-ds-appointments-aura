package com.wacds.appointments.publicbooking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Solicitud pública de reprogramación de una reserva.
 *
 * El cliente únicamente indica el nuevo inicio deseado.
 *
 * El backend NO confía en:
 *
 * - endAt enviado por el cliente;
 * - duración enviada por el cliente;
 * - businessId enviado por el cliente;
 * - professionalId enviado por el cliente;
 * - serviceId enviado por el cliente.
 *
 * Todos esos datos se obtienen de la Appointment asociada
 * al management token.
 *
 * La duración real del servicio y el nuevo endAt se calculan
 * en backend.
 */
public record PublicBookingRescheduleRequest(

    // =========================================================
    // NEW START
    // =========================================================

    /**
     * Nuevo inicio solicitado para la cita.
     *
     * Debe ser una fecha futura.
     *
     * La disponibilidad real se volverá a validar en backend.
     */
    @NotNull(
        message = "startAt es obligatorio."
    )
    @Future(
        message = "startAt debe estar en el futuro."
    )
    OffsetDateTime startAt

) {
}