package com.wacds.appointments.publicbooking.dto;

import com.wacds.appointments.appointment.Appointment;

import java.time.OffsetDateTime;

/**
 * Respuesta pública devuelta después de reprogramar
 * correctamente una reserva mediante management token.
 *
 * La Appointment sigue siendo la misma.
 *
 * Únicamente cambia su intervalo temporal.
 *
 * Este DTO no expone:
 *
 * - entidades JPA;
 * - tokenHash;
 * - credenciales internas;
 * - información administrativa;
 * - datos sensibles innecesarios del Customer.
 */
public record PublicBookingRescheduleResponse(

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

    Integer serviceDurationMinutes,

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
    // PREVIOUS TIME
    // =========================================================

    /**
     * Inicio anterior de la reserva.
     */
    OffsetDateTime previousStartAt,

    /**
     * Fin anterior de la reserva.
     */
    OffsetDateTime previousEndAt,

    // =========================================================
    // NEW TIME
    // =========================================================

    /**
     * Nuevo inicio efectivo de la reserva.
     */
    OffsetDateTime startAt,

    /**
     * Nuevo final efectivo de la reserva.
     */
    OffsetDateTime endAt,

    /**
     * Zona horaria IANA del negocio.
     *
     * Ejemplo:
     *
     * Europe/Madrid
     */
    String timezone,

    // =========================================================
    // RESCHEDULE
    // =========================================================

    /**
     * Momento en el que se ha realizado la reprogramación.
     *
     * Actualmente es un dato de respuesta y no implica
     * que exista una columna rescheduled_at persistida.
     */
    OffsetDateTime rescheduledAt,

    // =========================================================
    // MANAGEMENT
    // =========================================================

    /**
     * Indica si el management token continúa siendo válido
     * después de la reprogramación.
     *
     * En el diseño actual será true.
     */
    boolean managementAccessPreserved,

    // =========================================================
    // CONFIRMATION
    // =========================================================

    String message

) {
}