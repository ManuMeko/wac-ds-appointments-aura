package com.wacds.appointments.publicbooking.dto;

import com.wacds.appointments.appointment.Appointment;

import java.time.OffsetDateTime;

/**
 * Respuesta pública devuelta después de cancelar
 * correctamente una reserva mediante management token.
 *
 * Este DTO está diseñado para el flujo público y no expone:
 *
 * - entidades JPA;
 * - tokenHash;
 * - credenciales internas;
 * - datos sensibles del Customer;
 * - información administrativa.
 */
public record PublicBookingCancelResponse(

    // =========================================================
    // BOOKING
    // =========================================================

    Long appointmentId,

    Appointment.AppointmentStatus status,

    // =========================================================
    // BUSINESS
    // =========================================================

    Long businessId,

    String businessName,

    // =========================================================
    // SERVICE
    // =========================================================

    Long serviceId,

    String serviceName,

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    Long professionalId,

    String professionalName,

    // =========================================================
    // CUSTOMER
    // =========================================================

    String customerName,

    // =========================================================
    // ORIGINAL APPOINTMENT TIME
    // =========================================================

    OffsetDateTime startAt,

    OffsetDateTime endAt,

    String timezone,

    // =========================================================
    // CANCELLATION
    // =========================================================

    /**
     * Momento efectivo de cancelación.
     */
    OffsetDateTime cancelledAt,

    /**
     * Mensaje público asociado a la operación.
     */
    String message

) {
}