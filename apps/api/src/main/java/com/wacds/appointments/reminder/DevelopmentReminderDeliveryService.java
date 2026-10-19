package com.wacds.appointments.reminder;

import com.wacds.appointments.reminder.AppointmentReminder.ReminderChannel;
import com.wacds.appointments.reminder.AppointmentReminder.ReminderStatus;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Implementación de desarrollo del sistema de entrega
 * de recordatorios.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * No realiza ningún envío real.
 *
 * Su objetivo es permitir validar completamente:
 *
 * - scheduler;
 * - worker;
 * - transición PENDING / FAILED -> PROCESSING;
 * - transición PROCESSING -> SENT;
 * - providerMessageId;
 * - persistencia;
 * - reintentos;
 * - histórico de reprogramaciones;
 * - integración con AppointmentReminderService;
 *
 * sin depender todavía de proveedores externos.
 *
 * =========================================================
 * CANALES
 * =========================================================
 *
 * Esta implementación únicamente soporta:
 *
 * EMAIL
 *
 * En producción podrá sustituirse o convivir con:
 *
 * - EmailReminderDeliveryService;
 * - SmsReminderDeliveryService;
 * - WhatsAppReminderDeliveryService.
 *
 * =========================================================
 * V15 · APPOINTMENT OCCURRENCE
 * =========================================================
 *
 * Desde V15 cada reminder contiene:
 *
 * appointmentStartAt
 *
 * que representa el snapshot temporal de la Appointment
 * para la que ese reminder fue generado.
 *
 * Esto permite conservar correctamente el histórico:
 *
 * FIRST
 * appointmentStartAt = 11:20
 * status = CANCELLED
 *
 * FIRST
 * appointmentStartAt = 18:00
 * status = PROCESSING
 *
 * aunque ambos reminders pertenezcan a la misma Appointment.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * Este servicio NO debe registrar:
 *
 * - destination completa;
 * - management tokens;
 * - credenciales;
 * - secretos;
 * - payloads externos.
 */
@Slf4j
@Service
@Profile({
    "default",
    "dev"
})
public class DevelopmentReminderDeliveryService
    implements ReminderDeliveryService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String PROVIDER_PREFIX =
        "DEV-";

    // =========================================================
    // SUPPORT
    // =========================================================

    /**
     * Indica si este proveedor de desarrollo puede procesar
     * el canal solicitado.
     */
    @Override
    public boolean supports(
        ReminderChannel channel
    ) {

        return channel
            == ReminderChannel.EMAIL;
    }

    // =========================================================
    // DELIVER
    // =========================================================

    /**
     * Simula una entrega exitosa.
     *
     * El worker debe haber realizado previamente:
     *
     * PENDING / FAILED
     *
     *      ↓
     *
     * PROCESSING
     *
     * Este servicio representa exclusivamente la interacción
     * con el proveedor.
     *
     * No modifica directamente:
     *
     * - status;
     * - sentAt;
     * - attemptCount;
     * - lastAttemptAt;
     * - providerMessageId persistido.
     *
     * Esa responsabilidad pertenece a
     * AppointmentReminderService.
     */
    @Override
    public DeliveryResult deliver(
        AppointmentReminder reminder
    ) {

        validateReminder(
            reminder
        );

        // -----------------------------------------------------
        // CHANNEL SUPPORT
        // -----------------------------------------------------

        if (
            !supports(
                reminder.getChannel()
            )
        ) {

            return DeliveryResult.failed(
                "El canal "
                    + reminder.getChannel()
                    + " no está soportado por "
                    + "DevelopmentReminderDeliveryService."
            );
        }

        // -----------------------------------------------------
        // PROVIDER MESSAGE ID
        // -----------------------------------------------------

        String providerMessageId =
            PROVIDER_PREFIX
                + UUID.randomUUID();

        // -----------------------------------------------------
        // SIMULATED DELIVERY
        // -----------------------------------------------------

        /*
         * No se realiza ninguna llamada externa.
         *
         * El hecho de llegar hasta este punto representa una
         * entrega aceptada correctamente por el proveedor
         * simulado.
         */
        log.info(
            "Development reminder delivered. "
                + "reminderId={}, appointmentId={}, "
                + "appointmentStartAt={}, type={}, "
                + "channel={}, attemptCount={}, "
                + "providerMessageId={}",
            reminder.getId(),
            resolveAppointmentId(
                reminder
            ),
            reminder.getAppointmentStartAt(),
            reminder.getReminderType(),
            reminder.getChannel(),
            reminder.getAttemptCount(),
            providerMessageId
        );

        return DeliveryResult.sent(
            providerMessageId
        );
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    /**
     * Valida que el reminder contiene todo lo necesario antes
     * de simular una llamada a un proveedor externo.
     */
    private void validateReminder(
        AppointmentReminder reminder
    ) {

        if (
            reminder == null
        ) {

            throw new IllegalArgumentException(
                "AppointmentReminder es obligatorio."
            );
        }

        // -----------------------------------------------------
        // ID
        // -----------------------------------------------------

        if (
            reminder.getId() == null
                || reminder.getId() <= 0
        ) {

            throw new IllegalArgumentException(
                "El recordatorio no tiene un id válido."
            );
        }

        // -----------------------------------------------------
        // APPOINTMENT
        // -----------------------------------------------------

        if (
            reminder.getAppointment() == null
                || reminder
                    .getAppointment()
                    .getId() == null
                || reminder
                    .getAppointment()
                    .getId() <= 0
        ) {

            throw new IllegalArgumentException(
                "El recordatorio no tiene una Appointment válida."
            );
        }

        // -----------------------------------------------------
        // OCCURRENCE SNAPSHOT
        // -----------------------------------------------------

        OffsetDateTime appointmentStartAt =
            reminder.getAppointmentStartAt();

        if (
            appointmentStartAt == null
        ) {

            throw new IllegalArgumentException(
                "El recordatorio no contiene appointmentStartAt."
            );
        }

        // -----------------------------------------------------
        // TYPE
        // -----------------------------------------------------

        if (
            reminder.getReminderType() == null
        ) {

            throw new IllegalArgumentException(
                "El recordatorio no tiene tipo."
            );
        }

        // -----------------------------------------------------
        // CHANNEL
        // -----------------------------------------------------

        if (
            reminder.getChannel() == null
        ) {

            throw new IllegalArgumentException(
                "El recordatorio no tiene canal."
            );
        }

        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        /*
         * El proveedor nunca debe recibir directamente un
         * reminder PENDING, FAILED, SENT o CANCELLED.
         *
         * AppointmentReminderWorker debe haber realizado antes
         * la transición a PROCESSING.
         */
        if (
            reminder.getStatus()
                != ReminderStatus.PROCESSING
        ) {

            throw new IllegalStateException(
                "El recordatorio debe estar en estado PROCESSING "
                    + "antes de enviarse."
            );
        }

        // -----------------------------------------------------
        // SCHEDULED FOR
        // -----------------------------------------------------

        if (
            reminder.getScheduledFor() == null
        ) {

            throw new IllegalArgumentException(
                "El recordatorio no tiene scheduledFor."
            );
        }

        // -----------------------------------------------------
        // DESTINATION
        // -----------------------------------------------------

        if (
            reminder.getDestination() == null
                || reminder
                    .getDestination()
                    .isBlank()
        ) {

            throw new IllegalArgumentException(
                "El recordatorio no tiene destino."
            );
        }

        // -----------------------------------------------------
        // ATTEMPT COUNT
        // -----------------------------------------------------

        /*
         * markProcessing() debe haber incrementado attemptCount
         * antes de invocar este proveedor.
         */
        if (
            reminder.getAttemptCount() <= 0
        ) {

            throw new IllegalStateException(
                "El recordatorio no tiene un intento de envío válido."
            );
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Obtiene el appointmentId de forma defensiva para logging.
     */
    private Long resolveAppointmentId(
        AppointmentReminder reminder
    ) {

        if (
            reminder == null
                || reminder.getAppointment() == null
        ) {

            return null;
        }

        return reminder
            .getAppointment()
            .getId();
    }
}