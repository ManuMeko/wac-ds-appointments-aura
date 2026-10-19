package com.wacds.appointments.publicbooking.dto;

import com.wacds.appointments.appointment.Appointment;

import java.time.OffsetDateTime;

/**
 * Respuesta pública utilizada para consultar y gestionar
 * una reserva mediante una credencial válida.
 *
 * Este DTO no expone:
 *
 * - entidades JPA;
 * - Customer completo;
 * - tokenHash;
 * - credenciales internas;
 * - información administrativa;
 * - datos sensibles innecesarios.
 *
 * Está pensado para ser utilizado por el flujo público
 * de gestión de citas:
 *
 * - consultar;
 * - cancelar;
 * - reprogramar.
 */
public record PublicBookingManagementResponse(

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
    // TIME
    // =========================================================

    OffsetDateTime startAt,

    OffsetDateTime endAt,

    String timezone,

    // =========================================================
    // MANAGEMENT POLICIES
    // =========================================================

    /**
     * Indica si el negocio permite cancelación online.
     */
    boolean onlineCancellationEnabled,

    /**
     * Antelación mínima exigida para cancelar.
     */
    int minimumCancellationNoticeMinutes,

    /**
     * Indica si el negocio permite reprogramación online.
     */
    boolean onlineReschedulingEnabled,

    /**
     * Antelación mínima exigida para reprogramar.
     */
    int minimumRescheduleNoticeMinutes,

    // =========================================================
    // EFFECTIVE ACTIONS
    // =========================================================

    /**
     * Resultado efectivo después de combinar:
     *
     * - BusinessSettings;
     * - estado actual de Appointment;
     * - fecha/hora de la cita;
     * - margen mínimo de cancelación.
     */
    boolean cancellationAllowed,

    /**
     * Resultado efectivo después de combinar:
     *
     * - BusinessSettings;
     * - estado actual de Appointment;
     * - fecha/hora de la cita;
     * - margen mínimo de reprogramación.
     */
    boolean reschedulingAllowed,

    // =========================================================
    // DEADLINES
    // =========================================================

    /**
     * Fecha límite efectiva para cancelar online.
     *
     * Puede ser null si la cancelación online
     * está deshabilitada.
     */
    OffsetDateTime cancellationDeadline,

    /**
     * Fecha límite efectiva para reprogramar online.
     *
     * Puede ser null si la reprogramación online
     * está deshabilitada.
     */
    OffsetDateTime reschedulingDeadline

) {
}