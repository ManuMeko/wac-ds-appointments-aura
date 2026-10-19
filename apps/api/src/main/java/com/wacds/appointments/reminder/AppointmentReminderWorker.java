package com.wacds.appointments.reminder;

import com.wacds.appointments.reminder.AppointmentReminder.ReminderChannel;
import com.wacds.appointments.reminder.AppointmentReminder.ReminderStatus;
import com.wacds.appointments.reminder.ReminderDeliveryService.DeliveryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Worker encargado de procesar recordatorios cuyo momento
 * de envío ya ha llegado.
 *
 * =========================================================
 * RESPONSABILIDAD
 * =========================================================
 *
 * Este componente:
 *
 * 1. localiza recordatorios vencidos;
 * 2. descarta candidatos que ya no sean procesables;
 * 3. evita starvation por FAILED agotados;
 * 4. limita el tamaño efectivo del lote;
 * 5. vuelve a bloquear y releer cada reminder;
 * 6. realiza la transición a PROCESSING;
 * 7. resuelve el proveedor por canal;
 * 8. realiza la entrega;
 * 9. persiste SENT o FAILED;
 * 10. devuelve métricas básicas de ejecución.
 *
 * =========================================================
 * ESTADOS PROCESABLES
 * =========================================================
 *
 * Únicamente pueden entrar al flujo de entrega:
 *
 * PENDING
 * FAILED
 *
 * Nunca se procesan:
 *
 * PROCESSING
 * SENT
 * CANCELLED
 *
 * =========================================================
 * REINTENTOS
 * =========================================================
 *
 * Un recordatorio puede realizar como máximo:
 *
 * intento 1
 * intento 2
 * intento 3
 *
 * attemptCount se incrementa al realizar la transición
 * a PROCESSING.
 *
 * Un FAILED con attemptCount >= MAX_ATTEMPTS permanece
 * definitivamente FAILED y deja de ser procesado.
 *
 * =========================================================
 * V15 · HISTÓRICO DE OCURRENCIAS
 * =========================================================
 *
 * Desde V15 cada reminder mantiene:
 *
 * appointmentStartAt
 *
 * como snapshot histórico de la ocurrencia de Appointment
 * que originó ese reminder.
 *
 * Ejemplo:
 *
 * FIRST
 * appointmentStartAt = 11:20
 * CANCELLED
 *
 * FIRST
 * appointmentStartAt = 18:00
 * PENDING
 *
 * La decisión de cancelar reminders pertenecientes a
 * ocurrencias antiguas corresponde a
 * AppointmentReminderService.
 *
 * Antes de entregar, el worker vuelve a leer la cita:
 *
 * - si ya no está PENDING ni CONFIRMED, no envía;
 * - si startAt ya no coincide con appointmentStartAt,
 *   no envía.
 *
 * En ambos casos el reminder activo pasa a CANCELLED.
 *
 * =========================================================
 * PROVEEDORES
 * =========================================================
 *
 * El worker no conoce implementaciones concretas como:
 *
 * - SMTP;
 * - Amazon SES;
 * - Resend;
 * - Twilio;
 * - WhatsApp Cloud API.
 *
 * Trabaja exclusivamente contra ReminderDeliveryService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderWorker {

    // =========================================================
    // CONSTANTS
    // =========================================================

    /**
     * Máximo de reminders realmente procesados
     * en una ejecución.
     *
     * Los candidatos descartados no consumen capacidad.
     */
    private static final int MAX_BATCH_SIZE =
        50;

    /**
     * Número máximo de intentos reales de entrega.
     */
    private static final int MAX_ATTEMPTS =
        3;

    /**
     * Límite defensivo para mensajes persistidos/logueados.
     */
    private static final int MAX_ERROR_LENGTH =
        500;

    /**
     * Límite defensivo para valores incluidos en logs.
     */
    private static final int MAX_LOG_VALUE_LENGTH =
        300;

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AppointmentReminderService
        appointmentReminderService;

    /**
     * Spring inyecta todas las implementaciones disponibles
     * de ReminderDeliveryService.
     *
     * Actualmente:
     *
     * DevelopmentReminderDeliveryService
     *
     * Futuro:
     *
     * EmailReminderDeliveryService
     * SmsReminderDeliveryService
     * WhatsAppReminderDeliveryService
     */
    private final List<ReminderDeliveryService>
        deliveryServices;

    // =========================================================
    // PROCESS DUE REMINDERS
    // =========================================================

    /**
     * Procesa reminders cuyo scheduledFor ya ha vencido.
     *
     * Los FAILED que han agotado sus intentos se descartan
     * antes de consumir capacidad del batch.
     */
    public WorkerResult processDueReminders() {

        OffsetDateTime now =
            OffsetDateTime.now();

        List<AppointmentReminder> dueReminders =
            appointmentReminderService
                .findDueReminders(
                    now
                );

        if (
            dueReminders == null
                || dueReminders.isEmpty()
        ) {

            return WorkerResult.empty();
        }

        int processed = 0;
        int sent = 0;
        int failed = 0;
        int skipped = 0;

        int selectedForProcessing = 0;

        /*
         * No mantenemos una única transacción alrededor del lote.
         *
         * Cada transición persistente se delega en
         * AppointmentReminderService.
         *
         * De esta forma no mantenemos locks PostgreSQL abiertos
         * durante llamadas potencialmente lentas a proveedores.
         */
        for (
            AppointmentReminder candidate
            : dueReminders
        ) {

            if (
                selectedForProcessing
                    >= MAX_BATCH_SIZE
            ) {

                break;
            }

            // -------------------------------------------------
            // INVALID CANDIDATE
            // -------------------------------------------------

            if (
                candidate == null
                    || candidate.getId() == null
                    || candidate.getId() <= 0
            ) {

                skipped++;

                continue;
            }

            ReminderStatus candidateStatus =
                candidate.getStatus();

            // -------------------------------------------------
            // NON-PROCESSABLE STATUS
            // -------------------------------------------------

            if (
                candidateStatus
                    != ReminderStatus.PENDING
                    && candidateStatus
                        != ReminderStatus.FAILED
            ) {

                skipped++;

                continue;
            }

            // -------------------------------------------------
            // EXHAUSTED FAILED REMINDER
            // -------------------------------------------------

            /*
             * Un FAILED que ya haya alcanzado MAX_ATTEMPTS
             * no puede volver a pasar por markProcessing().
             *
             * Esto evita ciclos como:
             *
             * FAILED attemptCount=3
             * -> PROCESSING attemptCount=4
             * -> FAILED
             *
             * en cada tick del scheduler.
             */
            if (
                candidateStatus
                    == ReminderStatus.FAILED
                    && candidate.getAttemptCount()
                        >= MAX_ATTEMPTS
            ) {

                skipped++;

                log.debug(
                    "Reminder skipped because maximum attempts "
                        + "have been reached. reminderId={}, "
                        + "attemptCount={}, appointmentStartAt={}",
                    candidate.getId(),
                    candidate.getAttemptCount(),
                    candidate.getAppointmentStartAt()
                );

                continue;
            }

            Long reminderId =
                candidate.getId();

            /*
             * Solo los candidatos que realmente vamos a intentar
             * procesar consumen capacidad del batch.
             */
            selectedForProcessing++;

            try {

                ProcessingResult result =
                    processReminder(
                        reminderId
                    );

                switch (
                    result
                ) {

                    case SENT -> {

                        processed++;
                        sent++;
                    }

                    case FAILED -> {

                        processed++;
                        failed++;
                    }

                    case SKIPPED -> {

                        skipped++;
                    }
                }

            } catch (
                Exception exception
            ) {

                /*
                 * processReminder intenta controlar internamente
                 * todos los errores previsibles.
                 *
                 * Llegar aquí implica un fallo inesperado de
                 * infraestructura, persistencia o programación.
                 */
                processed++;
                failed++;

                log.error(
                    "Unexpected error processing reminder. "
                        + "reminderId={}, appointmentStartAt={}, "
                        + "error={}",
                    reminderId,
                    candidate.getAppointmentStartAt(),
                    safeErrorMessage(
                        exception
                    )
                );

                /*
                 * Intentamos dejar el reminder fuera de PROCESSING
                 * si la excepción ocurrió antes de completar
                 * correctamente el ciclo.
                 *
                 * AppointmentReminderService conserva la autoridad
                 * sobre las transiciones válidas y no debe alterar
                 * incorrectamente un SENT o CANCELLED.
                 */
                try {

                    appointmentReminderService
                        .markFailed(
                            reminderId,
                            safeErrorMessage(
                                exception
                            )
                        );

                } catch (
                    Exception markFailedException
                ) {

                    log.error(
                        "Could not mark reminder as FAILED after "
                            + "unexpected worker error. "
                            + "reminderId={}, error={}",
                        reminderId,
                        safeErrorMessage(
                            markFailedException
                        )
                    );
                }
            }
        }

        return new WorkerResult(
            processed,
            sent,
            failed,
            skipped
        );
    }

    // =========================================================
    // PROCESS SINGLE REMINDER
    // =========================================================

    /**
     * Procesa un reminder individual.
     *
     * Flujo nominal:
     *
     * PENDING / FAILED
     *
     *      ↓
     *
     * PROCESSING
     *
     *      ↓
     *
     * ReminderDeliveryService.deliver(...)
     *
     *      ↓
     *
     * SENT
     *
     * o:
     *
     * FAILED
     *
     * markProcessing() vuelve a leer/bloquear la fila y es la
     * autoridad que decide si todavía puede procesarse.
     */
    public ProcessingResult processReminder(
        Long reminderId
    ) {

        validateReminderId(
            reminderId
        );

        // -----------------------------------------------------
        // MARK PROCESSING / DATABASE RECHECK
        // -----------------------------------------------------

        AppointmentReminder reminder =
            appointmentReminderService
                .markProcessing(
                    reminderId
                );

        if (
            reminder == null
        ) {

            log.debug(
                "Reminder skipped because markProcessing returned "
                    + "null. reminderId={}",
                reminderId
            );

            return ProcessingResult.SKIPPED;
        }

        ReminderStatus currentStatus =
            reminder.getStatus();

        // -----------------------------------------------------
        // TERMINAL STATES
        // -----------------------------------------------------

        if (
            currentStatus
                == ReminderStatus.SENT
                || currentStatus
                    == ReminderStatus.CANCELLED
        ) {

            log.debug(
                "Reminder skipped because it is already terminal. "
                    + "reminderId={}, status={}",
                reminderId,
                currentStatus
            );

            return ProcessingResult.SKIPPED;
        }

        // -----------------------------------------------------
        // PROCESSING REQUIRED
        // -----------------------------------------------------

        if (
            currentStatus
                != ReminderStatus.PROCESSING
        ) {

            log.debug(
                "Reminder skipped because markProcessing did not "
                    + "leave it in PROCESSING. "
                    + "reminderId={}, status={}",
                reminderId,
                currentStatus
            );

            return ProcessingResult.SKIPPED;
        }

        // -----------------------------------------------------
        // APPOINTMENT STILL MATCHES
        // -----------------------------------------------------

        /*
         * markProcessing ya dejó el reminder en PROCESSING.
         *
         * Antes de contactar al proveedor se vuelve a leer
         * la cita. Una cancelación o un cambio de startAt
         * que todavía no hubiera retirado este reminder
         * no puede producir un envío.
         */
        if (
            !appointmentReminderService
                .reminderStillMatchesAppointment(
                    reminderId
                )
        ) {

            log.debug(
                "Reminder skipped because the appointment was "
                    + "cancelled or its start time changed. "
                    + "reminderId={}",
                reminderId
            );

            return ProcessingResult.SKIPPED;
        }

        // -----------------------------------------------------
        // OCCURRENCE SNAPSHOT
        // -----------------------------------------------------

        if (
            reminder.getAppointmentStartAt()
                == null
        ) {

            return failReminder(
                reminderId,
                reminder,
                "El recordatorio no contiene appointmentStartAt."
            );
        }

        // -----------------------------------------------------
        // MAX ATTEMPTS DEFENSIVE CHECK
        // -----------------------------------------------------

        /*
         * El filtrado habitual ocurre antes de markProcessing().
         *
         * Esta comprobación adicional protege llamadas manuales
         * o condiciones de carrera.
         *
         * Los intentos 1, 2 y 3 son válidos.
         * Un attemptCount > 3 no puede entregar.
         */
        if (
            reminder.getAttemptCount()
                > MAX_ATTEMPTS
        ) {

            String error =
                "Se ha alcanzado el número máximo "
                    + "de intentos de envío.";

            appointmentReminderService
                .markFailed(
                    reminderId,
                    error
                );

            log.warn(
                "Reminder delivery skipped because maximum "
                    + "attempts were exceeded. "
                    + "reminderId={}, attemptCount={}, "
                    + "appointmentStartAt={}",
                reminderId,
                reminder.getAttemptCount(),
                reminder.getAppointmentStartAt()
            );

            return ProcessingResult.FAILED;
        }

        // -----------------------------------------------------
        // CHANNEL
        // -----------------------------------------------------

        if (
            reminder.getChannel()
                == null
        ) {

            return failReminder(
                reminderId,
                reminder,
                "El recordatorio no tiene un canal "
                    + "de entrega configurado."
            );
        }

        // -----------------------------------------------------
        // DESTINATION
        // -----------------------------------------------------

        if (
            reminder.getDestination()
                == null
                || reminder
                    .getDestination()
                    .isBlank()
        ) {

            return failReminder(
                reminderId,
                reminder,
                "El recordatorio no tiene un destino "
                    + "de entrega válido."
            );
        }

        // -----------------------------------------------------
        // DELIVERY SERVICE
        // -----------------------------------------------------

        ReminderDeliveryService deliveryService =
            resolveDeliveryService(
                reminder.getChannel()
            );

        if (
            deliveryService == null
        ) {

            return failReminder(
                reminderId,
                reminder,
                "No existe ningún proveedor configurado "
                    + "para el canal "
                    + reminder.getChannel()
                    + "."
            );
        }

        // -----------------------------------------------------
        // DELIVER
        // -----------------------------------------------------

        DeliveryResult deliveryResult;

        try {

            deliveryResult =
                deliveryService.deliver(
                    reminder
                );

        } catch (
            Exception exception
        ) {

            String error =
                safeErrorMessage(
                    exception
                );

            appointmentReminderService
                .markFailed(
                    reminderId,
                    error
                );

            log.warn(
                "Reminder delivery failed with exception. "
                    + "reminderId={}, channel={}, attempt={}, "
                    + "appointmentStartAt={}, error={}",
                reminderId,
                reminder.getChannel(),
                reminder.getAttemptCount(),
                reminder.getAppointmentStartAt(),
                normalizeLogValue(
                    error
                )
            );

            return ProcessingResult.FAILED;
        }

        // -----------------------------------------------------
        // NULL PROVIDER RESULT
        // -----------------------------------------------------

        if (
            deliveryResult == null
        ) {

            return failReminder(
                reminderId,
                reminder,
                "El proveedor de recordatorios "
                    + "ha devuelto una respuesta nula."
            );
        }

        // -----------------------------------------------------
        // PROVIDER SUCCESS
        // -----------------------------------------------------

        if (
            deliveryResult.success()
        ) {

            /*
             * IMPORTANTE:
             *
             * A partir de este punto el proveedor puede haber
             * aceptado externamente el mensaje.
             *
             * No debemos ejecutar deliver() de nuevo dentro de
             * esta misma ejecución aunque la persistencia posterior
             * falle.
             *
             * Una futura integración de producción deberá utilizar
             * idempotencia del proveedor para conseguir garantías
             * más fuertes ante fallos entre proveedor y base de datos.
             */
            AppointmentReminder sentReminder;

            try {

                sentReminder =
                    appointmentReminderService
                        .markSent(
                            reminderId,
                            normalizeProviderMessageId(
                                deliveryResult
                                    .providerMessageId()
                            )
                        );

            } catch (
                Exception exception
            ) {

                /*
                 * El proveedor puede haber entregado correctamente,
                 * pero no hemos podido confirmar SENT en PostgreSQL.
                 *
                 * No repetimos la llamada al proveedor aquí.
                 *
                 * Propagamos la excepción para observabilidad y para
                 * que el mecanismo superior pueda recuperar el estado.
                 */
                log.error(
                    "Reminder provider reported success but SENT "
                        + "could not be persisted. "
                        + "reminderId={}, appointmentStartAt={}, "
                        + "error={}",
                    reminderId,
                    reminder.getAppointmentStartAt(),
                    safeErrorMessage(
                        exception
                    )
                );

                throw exception;
            }

            if (
                sentReminder == null
                    || !sentReminder.isSent()
            ) {

                /*
                 * El proveedor ya reportó éxito, por lo que no
                 * ejecutamos una segunda entrega.
                 *
                 * Dejamos constancia explícita de la inconsistencia.
                 */
                log.error(
                    "Reminder provider reported success but persisted "
                        + "reminder did not become SENT. "
                        + "reminderId={}, appointmentStartAt={}",
                    reminderId,
                    reminder.getAppointmentStartAt()
                );

                return ProcessingResult.SKIPPED;
            }

            log.info(
                "Reminder successfully processed. "
                    + "reminderId={}, channel={}, attempt={}, "
                    + "appointmentStartAt={}, providerMessageId={}",
                reminderId,
                reminder.getChannel(),
                reminder.getAttemptCount(),
                reminder.getAppointmentStartAt(),
                normalizeLogValue(
                    sentReminder.getProviderMessageId()
                )
            );

            return ProcessingResult.SENT;
        }

        // -----------------------------------------------------
        // PROVIDER FAILURE
        // -----------------------------------------------------

        String providerError =
            normalizeProviderError(
                deliveryResult.errorMessage()
            );

        appointmentReminderService
            .markFailed(
                reminderId,
                providerError
            );

        log.warn(
            "Reminder provider rejected delivery. "
                + "reminderId={}, channel={}, attempt={}, "
                + "appointmentStartAt={}, error={}",
            reminderId,
            reminder.getChannel(),
            reminder.getAttemptCount(),
            reminder.getAppointmentStartAt(),
            normalizeLogValue(
                providerError
            )
        );

        return ProcessingResult.FAILED;
    }

    // =========================================================
    // FAIL REMINDER
    // =========================================================

    /**
     * Marca de forma uniforme un reminder como FAILED
     * cuando detectamos un problema previo a la llamada
     * al proveedor.
     */
    private ProcessingResult failReminder(
        Long reminderId,
        AppointmentReminder reminder,
        String error
    ) {

        String normalizedError =
            normalizeErrorMessage(
                error
            );

        appointmentReminderService
            .markFailed(
                reminderId,
                normalizedError
            );

        log.warn(
            "Reminder processing failed before provider delivery. "
                + "reminderId={}, channel={}, attempt={}, "
                + "appointmentStartAt={}, error={}",
            reminderId,
            reminder != null
                ? reminder.getChannel()
                : null,
            reminder != null
                ? reminder.getAttemptCount()
                : null,
            reminder != null
                ? reminder.getAppointmentStartAt()
                : null,
            normalizeLogValue(
                normalizedError
            )
        );

        return ProcessingResult.FAILED;
    }

    // =========================================================
    // DELIVERY PROVIDER RESOLUTION
    // =========================================================

    /**
     * Localiza la primera implementación disponible que soporte
     * el canal solicitado.
     */
    private ReminderDeliveryService resolveDeliveryService(
        ReminderChannel channel
    ) {

        if (
            channel == null
                || deliveryServices == null
                || deliveryServices.isEmpty()
        ) {

            return null;
        }

        return deliveryServices
            .stream()
            .filter(
                service ->
                    service != null
                        && service.supports(
                            channel
                        )
            )
            .findFirst()
            .orElse(
                null
            );
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateReminderId(
        Long reminderId
    ) {

        if (
            reminderId == null
                || reminderId <= 0
        ) {

            throw new IllegalArgumentException(
                "reminderId no es válido."
            );
        }
    }

    // =========================================================
    // PROVIDER RESULT NORMALIZATION
    // =========================================================

    /**
     * Normaliza el providerMessageId antes de persistirlo.
     *
     * Puede ser null si el proveedor no proporciona uno.
     */
    private String normalizeProviderMessageId(
        String providerMessageId
    ) {

        if (
            providerMessageId == null
        ) {

            return null;
        }

        String normalized =
            providerMessageId.trim();

        if (
            normalized.isEmpty()
        ) {

            return null;
        }

        return normalized;
    }

    /**
     * Garantiza que un fallo de proveedor siempre tenga
     * un mensaje persistible y útil.
     */
    private String normalizeProviderError(
        String error
    ) {

        if (
            error == null
                || error.isBlank()
        ) {

            return "El proveedor rechazó el recordatorio "
                + "sin especificar el motivo.";
        }

        return normalizeErrorMessage(
            error
        );
    }

    /**
     * Limita defensivamente el contenido persistido en lastError.
     */
    private String normalizeErrorMessage(
        String value
    ) {

        if (
            value == null
                || value.isBlank()
        ) {

            return "Error de procesamiento no especificado.";
        }

        String normalized =
            value.trim();

        if (
            normalized.length()
                > MAX_ERROR_LENGTH
        ) {

            return normalized.substring(
                0,
                MAX_ERROR_LENGTH
            );
        }

        return normalized;
    }

    // =========================================================
    // SAFE ERROR HANDLING
    // =========================================================

    /**
     * Obtiene un mensaje reducido para persistencia
     * y observabilidad.
     *
     * No se incluyen deliberadamente:
     *
     * - tokens;
     * - credenciales;
     * - request completos;
     * - payloads de proveedor.
     */
    private String safeErrorMessage(
        Exception exception
    ) {

        if (
            exception == null
        ) {

            return "Error de procesamiento no especificado.";
        }

        String message =
            exception.getMessage();

        if (
            message == null
                || message.isBlank()
        ) {

            return normalizeErrorMessage(
                exception
                    .getClass()
                    .getSimpleName()
            );
        }

        return normalizeErrorMessage(
            message
        );
    }

    /**
     * Reduce valores destinados exclusivamente a logs.
     */
    private String normalizeLogValue(
        String value
    ) {

        if (
            value == null
                || value.isBlank()
        ) {

            return "unknown";
        }

        String normalized =
            value.trim();

        if (
            normalized.length()
                > MAX_LOG_VALUE_LENGTH
        ) {

            return normalized.substring(
                0,
                MAX_LOG_VALUE_LENGTH
            );
        }

        return normalized;
    }

    // =========================================================
    // RESULT ENUM
    // =========================================================

    public enum ProcessingResult {

        SENT,

        FAILED,

        SKIPPED
    }

    // =========================================================
    // WORKER RESULT
    // =========================================================

    /**
     * Resumen de una ejecución del worker.
     */
    public record WorkerResult(

        int processed,

        int sent,

        int failed,

        int skipped

    ) {

        /**
         * Resultado vacío reutilizable semánticamente.
         */
        public static WorkerResult empty() {

            return new WorkerResult(
                0,
                0,
                0,
                0
            );
        }
    }
}