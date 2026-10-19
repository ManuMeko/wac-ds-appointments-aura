package com.wacds.appointments.business.dto;

import java.time.OffsetDateTime;

/**
 * DTO de salida para la configuración operativa
 * de un Business.
 *
 * Esta respuesta representa únicamente configuración
 * y políticas del negocio relacionadas con:
 *
 * - ventana de reserva;
 * - generación de slots;
 * - buffers;
 * - confirmación automática;
 * - cancelación online;
 * - reprogramación online;
 * - recordatorios;
 * - control optimista de concurrencia;
 * - auditoría.
 *
 * IMPORTANTE:
 *
 * Este DTO no debe contener lógica de negocio.
 *
 * La lógica debe residir en:
 *
 * - BusinessSettings
 * - BusinessSettingsService
 * - servicios operativos que consuman estos ajustes
 */
public record BusinessSettingsResponse(

    // =========================================================
    // IDENTITY
    // =========================================================

    /**
     * Identificador de la configuración.
     */
    Long id,

    /**
     * Identificador del Business propietario
     * de esta configuración.
     */
    Long businessId,

    // =========================================================
    // BOOKING WINDOW
    // =========================================================

    /**
     * Antelación mínima necesaria para realizar
     * una nueva reserva.
     *
     * Unidad: minutos.
     *
     * Ejemplo:
     *
     * 120 = 2 horas.
     */
    int minimumBookingNoticeMinutes,

    /**
     * Número máximo de días hacia el futuro
     * en los que se permite reservar.
     *
     * Ejemplo:
     *
     * 60 = reservas permitidas hasta 60 días
     * desde el momento actual.
     */
    int bookingHorizonDays,

    // =========================================================
    // SLOT GENERATION
    // =========================================================

    /**
     * Intervalo utilizado para generar posibles
     * horas de inicio.
     *
     * Unidad: minutos.
     *
     * Ejemplo:
     *
     * 15 = 09:00, 09:15, 09:30...
     *
     * 20 = 09:00, 09:20, 09:40...
     */
    int slotIntervalMinutes,

    // =========================================================
    // DEFAULT BUFFERS
    // =========================================================

    /**
     * Margen predeterminado antes de una cita.
     *
     * Unidad: minutos.
     *
     * Este tiempo forma parte de la ocupación operativa
     * de la agenda aunque no forme parte de la duración
     * visible del servicio.
     */
    int defaultBufferBeforeMinutes,

    /**
     * Margen predeterminado después de una cita.
     *
     * Unidad: minutos.
     *
     * Este tiempo forma parte de la ocupación operativa
     * de la agenda aunque no forme parte de la duración
     * visible del servicio.
     */
    int defaultBufferAfterMinutes,

    // =========================================================
    // CONFIRMATION
    // =========================================================

    /**
     * Determina el estado inicial de las nuevas reservas.
     *
     * true:
     * nuevas citas confirmadas automáticamente.
     *
     * false:
     * nuevas citas pueden quedar en estado pendiente.
     */
    boolean autoConfirmAppointments,

    // =========================================================
    // ONLINE CANCELLATION
    // =========================================================

    /**
     * Permite o impide que el cliente cancele
     * una cita desde el flujo online.
     */
    boolean onlineCancellationEnabled,

    /**
     * Antelación mínima necesaria para permitir
     * una cancelación online.
     *
     * Unidad: minutos.
     *
     * Ejemplo:
     *
     * 1440 = 24 horas.
     */
    int minimumCancellationNoticeMinutes,

    // =========================================================
    // ONLINE RESCHEDULING
    // =========================================================

    /**
     * Permite o impide que el cliente reprograme
     * una cita desde el flujo online.
     */
    boolean onlineReschedulingEnabled,

    /**
     * Antelación mínima necesaria para permitir
     * una reprogramación online.
     *
     * Unidad: minutos.
     *
     * Ejemplo:
     *
     * 1440 = 24 horas.
     */
    int minimumRescheduleNoticeMinutes,

    // =========================================================
    // REMINDERS
    // =========================================================

    /**
     * Activa o desactiva el sistema general
     * de recordatorios.
     */
    boolean remindersEnabled,

    /**
     * Minutos antes de la cita en los que debe
     * ejecutarse el primer recordatorio.
     */
    int firstReminderMinutesBefore,

    /**
     * Activa o desactiva un segundo recordatorio.
     *
     * Este valor solo tiene efecto cuando
     * remindersEnabled = true.
     */
    boolean secondReminderEnabled,

    /**
     * Minutos antes de la cita en los que debe
     * ejecutarse el segundo recordatorio.
     */
    int secondReminderMinutesBefore,

    // =========================================================
    // OPTIMISTIC LOCKING
    // =========================================================

    /**
     * Versión utilizada para control optimista
     * de concurrencia.
     *
     * El cliente debe reenviar este valor al realizar
     * modificaciones de BusinessSettings.
     */
    Long version,

    // =========================================================
    // AUDIT
    // =========================================================

    /**
     * Fecha de creación de la configuración.
     */
    OffsetDateTime createdAt,

    /**
     * Fecha de última actualización.
     */
    OffsetDateTime updatedAt

) {
}