package com.wacds.appointments.reminder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Scheduler automático del sistema de recordatorios.
 *
 * =========================================================
 * RESPONSABILIDAD
 * =========================================================
 *
 * Ejecuta periódicamente AppointmentReminderWorker.
 *
 * El scheduler NO:
 *
 * - genera recordatorios;
 * - envía emails directamente;
 * - modifica Appointment;
 * - decide qué reminders deben existir;
 * - cambia estados manualmente;
 * - conoce proveedores externos.
 *
 * Únicamente dispara periódicamente el procesamiento
 * automático de reminders vencidos.
 *
 * =========================================================
 * ARQUITECTURA
 * =========================================================
 *
 * Scheduler
 *    ↓
 * AppointmentReminderWorker
 *    ↓
 * AppointmentReminderService
 *    ↓
 * ReminderDeliveryService
 *    ↓
 * proveedor concreto
 *
 * =========================================================
 * CONCURRENCIA
 * =========================================================
 *
 * fixedDelay garantiza que, dentro de esta instancia de la
 * aplicación, una nueva ejecución no comience hasta que haya
 * finalizado la anterior y haya transcurrido el delay indicado.
 *
 * Si en el futuro existen varias instancias de la API ejecutando
 * simultáneamente, la protección definitiva de cada reminder
 * continúa realizándose en la capa de servicio/repositorio mediante
 * bloqueo pesimista antes de procesarlo.
 *
 * =========================================================
 * CONFIGURACIÓN
 * =========================================================
 *
 * Scheduler habilitado:
 *
 * app.reminders.scheduler.enabled=true
 *
 * Delay entre ejecuciones:
 *
 * app.reminders.scheduler.fixed-delay-ms=60000
 *
 * Delay inicial:
 *
 * app.reminders.scheduler.initial-delay-ms=10000
 *
 * Si no se declara ninguna propiedad:
 *
 * - enabled = true
 * - fixedDelay = 60000 ms
 * - initialDelay = 10000 ms
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.reminders.scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AppointmentReminderScheduler {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_SAFE_LOG_MESSAGE_LENGTH =
        500;

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AppointmentReminderWorker
        appointmentReminderWorker;

    // =========================================================
    // PROCESS DUE REMINDERS
    // =========================================================

    /**
     * Ejecuta periódicamente AppointmentReminderWorker.
     *
     * fixedDelay:
     *
     * la siguiente ejecución comienza después de que haya
     * terminado la ejecución anterior y haya transcurrido
     * el número configurado de milisegundos.
     *
     * Por defecto:
     *
     * 60 segundos.
     *
     * initialDelay:
     *
     * evita ejecutar el worker inmediatamente durante el
     * arranque de Spring Boot.
     *
     * Por defecto:
     *
     * 10 segundos.
     */
    @Scheduled(
        fixedDelayString =
            "${app.reminders.scheduler.fixed-delay-ms:60000}",
        initialDelayString =
            "${app.reminders.scheduler.initial-delay-ms:10000}"
    )
    public void processDueReminders() {

        OffsetDateTime startedAt =
            OffsetDateTime.now();

        long startedAtNanos =
            System.nanoTime();

        try {

            AppointmentReminderWorker.WorkerResult result =
                appointmentReminderWorker
                    .processDueReminders();

            // -------------------------------------------------
            // DEFENSIVE NULL RESULT
            // -------------------------------------------------

            if (
                result == null
            ) {

                log.warn(
                    "Reminder scheduler execution returned "
                        + "a null WorkerResult."
                );

                return;
            }

            // -------------------------------------------------
            // EXECUTION DURATION
            // -------------------------------------------------

            long durationMs =
                calculateDurationMillis(
                    startedAtNanos
                );

            // -------------------------------------------------
            // ACTIVITY LOG
            // -------------------------------------------------

            /*
             * Evitamos escribir INFO cada minuto cuando no existe
             * absolutamente ningún reminder que procesar.
             *
             * Esto reduce ruido de logs en producción.
             */
            if (
                hasActivity(
                    result
                )
            ) {

                log.info(
                    "Reminder scheduler execution completed. "
                        + "startedAt={}, durationMs={}, "
                        + "processed={}, sent={}, failed={}, "
                        + "skipped={}",
                    startedAt,
                    durationMs,
                    result.processed(),
                    result.sent(),
                    result.failed(),
                    result.skipped()
                );

            } else {

                /*
                 * DEBUG permite verificar que el scheduler sigue
                 * ejecutándose sin llenar los logs normales.
                 */
                log.debug(
                    "Reminder scheduler execution completed "
                        + "without pending work. "
                        + "startedAt={}, durationMs={}",
                    startedAt,
                    durationMs
                );
            }

        } catch (
            Exception exception
        ) {

            long durationMs =
                calculateDurationMillis(
                    startedAtNanos
                );

            /*
             * Una excepción del worker no debe matar el scheduler.
             *
             * Spring podrá volver a ejecutar este método en el
             * siguiente ciclo fixedDelay.
             */
            log.error(
                "Unexpected reminder scheduler error. "
                    + "startedAt={}, durationMs={}, "
                    + "exceptionType={}, error={}",
                startedAt,
                durationMs,
                resolveExceptionType(
                    exception
                ),
                safeMessage(
                    exception
                )
            );
        }
    }

    // =========================================================
    // ACTIVITY
    // =========================================================

    /**
     * Determina si una ejecución realizó alguna actividad
     * relevante que merezca aparecer en INFO.
     */
    private boolean hasActivity(
        AppointmentReminderWorker.WorkerResult result
    ) {

        if (
            result == null
        ) {

            return false;
        }

        return result.processed() > 0
            || result.sent() > 0
            || result.failed() > 0
            || result.skipped() > 0;
    }

    // =========================================================
    // EXECUTION DURATION
    // =========================================================

    /**
     * Calcula la duración monotónica de la ejecución.
     *
     * Utilizamos System.nanoTime() para medir duración porque
     * no depende de posibles ajustes del reloj del sistema.
     */
    private long calculateDurationMillis(
        long startedAtNanos
    ) {

        long elapsedNanos =
            System.nanoTime()
                - startedAtNanos;

        if (
            elapsedNanos <= 0
        ) {

            return 0L;
        }

        return Duration
            .ofNanos(
                elapsedNanos
            )
            .toMillis();
    }

    // =========================================================
    // SAFE LOGGING
    // =========================================================

    /**
     * Obtiene exclusivamente un mensaje reducido y seguro
     * para observabilidad.
     *
     * No serializamos aquí:
     *
     * - stack traces completos;
     * - tokens;
     * - credenciales;
     * - payloads;
     * - cuerpos de peticiones;
     * - secretos de proveedores.
     */
    private String safeMessage(
        Exception exception
    ) {

        if (
            exception == null
        ) {

            return "Unknown scheduler error.";
        }

        String message =
            exception.getMessage();

        if (
            message == null
                || message.isBlank()
        ) {

            return exception
                .getClass()
                .getSimpleName();
        }

        String normalized =
            message.trim();

        if (
            normalized.length()
                > MAX_SAFE_LOG_MESSAGE_LENGTH
        ) {

            return normalized.substring(
                0,
                MAX_SAFE_LOG_MESSAGE_LENGTH
            );
        }

        return normalized;
    }

    /**
     * Devuelve únicamente el nombre simple de la excepción.
     */
    private String resolveExceptionType(
        Exception exception
    ) {

        if (
            exception == null
        ) {

            return "UnknownException";
        }

        String simpleName =
            exception
                .getClass()
                .getSimpleName();

        if (
            simpleName == null
                || simpleName.isBlank()
        ) {

            return "Exception";
        }

        return simpleName;
    }
}