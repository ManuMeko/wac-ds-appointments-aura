package com.wacds.appointments.business.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BusinessSettingsUpdateRequest(

    // =========================================================
    // BOOKING WINDOW
    // =========================================================

    /**
     * Antelación mínima para crear una reserva.
     *
     * Unidad: minutos.
     *
     * 0 = reserva inmediata permitida.
     *
     * Máximo:
     * 525600 = 365 días.
     */
    @Min(
        value = 0,
        message = "minimumBookingNoticeMinutes no puede ser negativo."
    )
    @Max(
        value = 525600,
        message = "minimumBookingNoticeMinutes no puede superar 525600 minutos."
    )
    Integer minimumBookingNoticeMinutes,

    /**
     * Máximo número de días hacia el futuro
     * en los que se permite reservar.
     */
    @Min(
        value = 1,
        message = "bookingHorizonDays debe ser al menos 1."
    )
    @Max(
        value = 730,
        message = "bookingHorizonDays no puede superar 730 días."
    )
    Integer bookingHorizonDays,

    // =========================================================
    // SLOT GENERATION
    // =========================================================

    /**
     * Intervalo utilizado para generar posibles
     * horas de inicio.
     *
     * Unidad: minutos.
     */
    @Min(
        value = 5,
        message = "slotIntervalMinutes debe ser como mínimo 5."
    )
    @Max(
        value = 240,
        message = "slotIntervalMinutes no puede superar 240 minutos."
    )
    Integer slotIntervalMinutes,

    // =========================================================
    // DEFAULT BUFFERS
    // =========================================================

    @Min(
        value = 0,
        message = "defaultBufferBeforeMinutes no puede ser negativo."
    )
    @Max(
        value = 240,
        message = "defaultBufferBeforeMinutes no puede superar 240 minutos."
    )
    Integer defaultBufferBeforeMinutes,

    @Min(
        value = 0,
        message = "defaultBufferAfterMinutes no puede ser negativo."
    )
    @Max(
        value = 240,
        message = "defaultBufferAfterMinutes no puede superar 240 minutos."
    )
    Integer defaultBufferAfterMinutes,

    // =========================================================
    // CONFIRMATION
    // =========================================================

    /**
     * true:
     * las nuevas reservas se confirman automáticamente.
     *
     * false:
     * podrán permanecer pendientes.
     */
    Boolean autoConfirmAppointments,

    // =========================================================
    // ONLINE CANCELLATION
    // =========================================================

    Boolean onlineCancellationEnabled,

    /**
     * Antelación mínima para cancelación online.
     *
     * Unidad: minutos.
     */
    @Min(
        value = 0,
        message = "minimumCancellationNoticeMinutes no puede ser negativo."
    )
    @Max(
        value = 525600,
        message = "minimumCancellationNoticeMinutes no puede superar 525600 minutos."
    )
    Integer minimumCancellationNoticeMinutes,

    // =========================================================
    // ONLINE RESCHEDULING
    // =========================================================

    Boolean onlineReschedulingEnabled,

    /**
     * Antelación mínima para reprogramación online.
     *
     * Unidad: minutos.
     */
    @Min(
        value = 0,
        message = "minimumRescheduleNoticeMinutes no puede ser negativo."
    )
    @Max(
        value = 525600,
        message = "minimumRescheduleNoticeMinutes no puede superar 525600 minutos."
    )
    Integer minimumRescheduleNoticeMinutes,

    // =========================================================
    // REMINDERS
    // =========================================================

    Boolean remindersEnabled,

    /**
     * Minutos antes de la cita para el primer recordatorio.
     */
    @Min(
        value = 1,
        message = "firstReminderMinutesBefore debe ser mayor que 0."
    )
    @Max(
        value = 525600,
        message = "firstReminderMinutesBefore no puede superar 525600 minutos."
    )
    Integer firstReminderMinutesBefore,

    Boolean secondReminderEnabled,

    /**
     * Minutos antes de la cita para el segundo recordatorio.
     */
    @Min(
        value = 1,
        message = "secondReminderMinutesBefore debe ser mayor que 0."
    )
    @Max(
        value = 525600,
        message = "secondReminderMinutesBefore no puede superar 525600 minutos."
    )
    Integer secondReminderMinutesBefore,

    // =========================================================
    // OPTIMISTIC LOCKING
    // =========================================================

    /**
     * Versión esperada por el cliente.
     *
     * Se utilizará para detectar actualizaciones concurrentes.
     */
    @Min(
        value = 0,
        message = "version no puede ser negativa."
    )
    Long version

) {
}