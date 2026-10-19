package com.wacds.appointments.reminder;

import com.wacds.appointments.appointment.Appointment;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.business.settings.BusinessSettingsRepository;
import com.wacds.appointments.customer.Customer;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.reminder.AppointmentReminder.ReminderChannel;
import com.wacds.appointments.reminder.AppointmentReminder.ReminderStatus;
import com.wacds.appointments.reminder.AppointmentReminder.ReminderType;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AppointmentReminderService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_ERROR_LENGTH =
        2000;

    private static final List<ReminderStatus> ACTIVE_STATUSES =
        List.of(
            ReminderStatus.PENDING,
            ReminderStatus.PROCESSING,
            ReminderStatus.FAILED
        );

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AppointmentReminderRepository
        appointmentReminderRepository;

    private final BusinessSettingsRepository
        businessSettingsRepository;

    // =========================================================
    // SYNCHRONIZE REMINDERS FOR APPOINTMENT
    // =========================================================

    /**
     * Sincroniza los recordatorios correspondientes al estado
     * ACTUAL de una Appointment.
     *
     * Este método debe utilizarse:
     *
     * - después de crear una cita;
     * - después de reprogramar una cita;
     * - cuando cambien settings relevantes.
     *
     * Desde V15 cada ocurrencia de reminder queda identificada por:
     *
     * appointment_id
     * + reminder_type
     * + appointment_start_at
     *
     * Esto significa que una Appointment puede conservar, por
     * ejemplo, varios FIRST históricos pertenecientes a distintos
     * horarios después de varias reprogramaciones.
     *
     * Reglas:
     *
     * - PENDING / CONFIRMED pueden generar reminders;
     * - CANCELLED / COMPLETED / NO_SHOW no generan reminders;
     * - si remindersEnabled=false se cancelan reminders activos;
     * - FIRST se programa cuando reminders están habilitados;
     * - SECOND depende de secondReminderEnabled;
     * - SENT nunca se modifica;
     * - reminders de horarios anteriores se conservan;
     * - reminders activos de horarios anteriores pasan a CANCELLED;
     * - la ocurrencia del horario actual se reutiliza si ya existe;
     * - nunca se borra histórico para sincronizar.
     */
    @Transactional
    public void synchronizeForAppointment(
        Appointment appointment
    ) {

        validateAppointment(
            appointment
        );

        Long appointmentId =
            appointment.getId();

        Business business =
            appointment.getBusiness();

        Long businessId =
            business.getId();

        BusinessSettings settings =
            requireBusinessSettings(
                businessId
            );

        validateBusinessSettings(
            settings,
            businessId
        );

        // -----------------------------------------------------
        // TERMINAL APPOINTMENT
        // -----------------------------------------------------

        if (
            !isReminderEligibleStatus(
                appointment.getStatus()
            )
        ) {

            cancelActiveReminders(
                appointmentId
            );

            return;
        }

        // -----------------------------------------------------
        // REMINDERS DISABLED
        // -----------------------------------------------------

        if (
            !settings.isRemindersEnabled()
        ) {

            cancelActiveReminders(
                appointmentId
            );

            return;
        }

        // -----------------------------------------------------
        // BUSINESS TIMEZONE
        // -----------------------------------------------------

        ZoneId businessZone =
            resolveBusinessZone(
                business
            );

        OffsetDateTime appointmentStartAt =
            appointment
                .getStartAt()
                .atZoneSameInstant(
                    businessZone
                )
                .toOffsetDateTime();

        OffsetDateTime now =
            OffsetDateTime.now(
                businessZone
            );

        // -----------------------------------------------------
        // CANCEL ACTIVE REMINDERS FROM PREVIOUS OCCURRENCES
        // -----------------------------------------------------

        /*
         * Una reprogramación NO elimina reminders anteriores.
         *
         * Ejemplo:
         *
         * FIRST 11:20 SENT
         * FIRST 16:00 PENDING
         *
         * SENT permanece como histórico.
         *
         * Si un reminder del horario anterior todavía estaba
         * PENDING / PROCESSING / FAILED, pasa a CANCELLED.
         */
        cancelActiveRemindersFromPreviousOccurrences(
            appointmentId,
            appointmentStartAt
        );

        // -----------------------------------------------------
        // FIRST REMINDER
        // -----------------------------------------------------

        OffsetDateTime firstScheduledFor =
            appointmentStartAt.minusMinutes(
                settings.getFirstReminderMinutesBefore()
            );

        synchronizeReminderOccurrence(
            appointment,
            appointmentStartAt,
            ReminderType.FIRST,
            ReminderChannel.EMAIL,
            firstScheduledFor,
            now,
            true
        );

        // -----------------------------------------------------
        // SECOND REMINDER
        // -----------------------------------------------------

        if (
            settings.isSecondReminderEnabled()
        ) {

            OffsetDateTime secondScheduledFor =
                appointmentStartAt.minusMinutes(
                    settings.getSecondReminderMinutesBefore()
                );

            synchronizeReminderOccurrence(
                appointment,
                appointmentStartAt,
                ReminderType.SECOND,
                ReminderChannel.EMAIL,
                secondScheduledFor,
                now,
                true
            );

        } else {

            /*
             * Puede existir un SECOND para el mismo startAt si
             * anteriormente estaba habilitado y los settings han
             * cambiado.
             *
             * No lo borramos: si todavía está activo se cancela.
             */
            synchronizeReminderOccurrence(
                appointment,
                appointmentStartAt,
                ReminderType.SECOND,
                ReminderChannel.EMAIL,
                null,
                now,
                false
            );
        }
    }

    // =========================================================
    // SYNCHRONIZE ONE REMINDER OCCURRENCE
    // =========================================================

    /**
     * Sincroniza FIRST o SECOND para una ocurrencia temporal
     * concreta de la Appointment.
     *
     * La ocurrencia se identifica mediante:
     *
     * appointmentId
     * + reminderType
     * + appointmentStartAt
     *
     * Si ya existe:
     *
     * - SENT -> permanece intacto;
     * - PENDING / PROCESSING / FAILED / CANCELLED -> puede
     *   actualizarse o reactivarse si el reminder sigue siendo
     *   relevante.
     *
     * Si ya no debe existir:
     *
     * - SENT -> permanece intacto;
     * - cualquier otro estado -> CANCELLED.
     */
    private void synchronizeReminderOccurrence(
        Appointment appointment,
        OffsetDateTime appointmentStartAt,
        ReminderType reminderType,
        ReminderChannel channel,
        OffsetDateTime scheduledFor,
        OffsetDateTime now,
        boolean enabled
    ) {

        if (
            appointment == null
                || appointment.getId() == null
        ) {

            throw new IllegalArgumentException(
                "Appointment es obligatoria."
            );
        }

        if (
            appointmentStartAt == null
        ) {

            throw new IllegalArgumentException(
                "appointmentStartAt es obligatorio."
            );
        }

        if (
            reminderType == null
        ) {

            throw new IllegalArgumentException(
                "reminderType es obligatorio."
            );
        }

        if (
            channel == null
        ) {

            throw new IllegalArgumentException(
                "channel es obligatorio."
            );
        }

        if (
            now == null
        ) {

            throw new IllegalArgumentException(
                "now es obligatorio."
            );
        }

        Long appointmentId =
            appointment.getId();

        Optional<AppointmentReminder> existingOptional =
            appointmentReminderRepository
                .findByAppointmentIdAndReminderTypeAndAppointmentStartAt(
                    appointmentId,
                    reminderType,
                    appointmentStartAt
                );

        // -----------------------------------------------------
        // DISABLED OCCURRENCE
        // -----------------------------------------------------

        if (
            !enabled
        ) {

            existingOptional.ifPresent(
                reminder -> {

                    if (
                        !reminder.isSent()
                            && !reminder.isCancelled()
                    ) {

                        reminder.cancel();

                        appointmentReminderRepository.save(
                            reminder
                        );
                    }
                }
            );

            return;
        }

        // -----------------------------------------------------
        // SCHEDULED TIME
        // -----------------------------------------------------

        if (
            scheduledFor == null
        ) {

            throw new IllegalArgumentException(
                "scheduledFor es obligatorio "
                    + "para un recordatorio habilitado."
            );
        }

        /*
         * Si el momento programado ya ha pasado, no generamos
         * un reminder atrasado.
         *
         * Si ya existía uno no enviado para esta misma ocurrencia,
         * se cancela en lugar de eliminarlo.
         */
        if (
            !scheduledFor.isAfter(
                now
            )
        ) {

            existingOptional.ifPresent(
                reminder -> {

                    if (
                        !reminder.isSent()
                            && !reminder.isCancelled()
                    ) {

                        reminder.cancel();

                        appointmentReminderRepository.save(
                            reminder
                        );
                    }
                }
            );

            return;
        }

        // -----------------------------------------------------
        // DESTINATION
        // -----------------------------------------------------

        Customer customer =
            appointment.getCustomer();

        String destination =
            resolveEmailDestination(
                customer
            );

        /*
         * Si no existe email todavía, no creamos un reminder
         * EMAIL imposible de entregar.
         *
         * Si ya existía uno no enviado, se cancela.
         */
        if (
            destination == null
        ) {

            existingOptional.ifPresent(
                reminder -> {

                    if (
                        !reminder.isSent()
                            && !reminder.isCancelled()
                    ) {

                        reminder.cancel();

                        appointmentReminderRepository.save(
                            reminder
                        );
                    }
                }
            );

            return;
        }

        // -----------------------------------------------------
        // EXISTING OCCURRENCE
        // -----------------------------------------------------

        if (
            existingOptional.isPresent()
        ) {

            AppointmentReminder existing =
                existingOptional.get();

            /*
             * Un recordatorio SENT forma parte del histórico.
             *
             * Nunca se reprograma, reactiva ni altera.
             */
            if (
                existing.isSent()
            ) {

                return;
            }

            /*
             * Estamos sincronizando exactamente la misma
             * ocurrencia temporal.
             *
             * Puede haber sido:
             *
             * - PENDING;
             * - PROCESSING;
             * - FAILED;
             * - CANCELLED por un cambio previo de settings.
             *
             * La dejamos nuevamente preparada según la
             * configuración actual.
             */
            existing.setChannel(
                channel
            );

            existing.setStatus(
                ReminderStatus.PENDING
            );

            existing.setScheduledFor(
                scheduledFor
            );

            existing.setDestination(
                destination
            );

            existing.setSentAt(
                null
            );

            existing.setLastAttemptAt(
                null
            );

            existing.setAttemptCount(
                0
            );

            existing.setProviderMessageId(
                null
            );

            existing.setLastError(
                null
            );

            appointmentReminderRepository.save(
                existing
            );

            return;
        }

        // -----------------------------------------------------
        // CREATE NEW OCCURRENCE
        // -----------------------------------------------------

        AppointmentReminder reminder =
            AppointmentReminder
                .builder()
                .appointment(
                    appointment
                )
                .appointmentStartAt(
                    appointmentStartAt
                )
                .reminderType(
                    reminderType
                )
                .channel(
                    channel
                )
                .status(
                    ReminderStatus.PENDING
                )
                .scheduledFor(
                    scheduledFor
                )
                .destination(
                    destination
                )
                .attemptCount(
                    0
                )
                .build();

        appointmentReminderRepository.save(
            reminder
        );
    }

    // =========================================================
    // CANCEL ACTIVE REMINDERS FROM PREVIOUS OCCURRENCES
    // =========================================================

    /**
     * Cancela únicamente reminders activos pertenecientes a
     * horarios anteriores de la misma Appointment.
     *
     * Los reminders del startAt actual se dejan para que
     * synchronizeReminderOccurrence(...) decida si deben
     * mantenerse, actualizarse o cancelarse.
     */
    private void cancelActiveRemindersFromPreviousOccurrences(
        Long appointmentId,
        OffsetDateTime currentAppointmentStartAt
    ) {

        validateAppointmentId(
            appointmentId
        );

        if (
            currentAppointmentStartAt == null
        ) {

            throw new IllegalArgumentException(
                "currentAppointmentStartAt es obligatorio."
            );
        }

        List<AppointmentReminder> activeReminders =
            appointmentReminderRepository
                .findAllByAppointmentIdAndStatuses(
                    appointmentId,
                    ACTIVE_STATUSES
                );

        if (
            activeReminders == null
                || activeReminders.isEmpty()
        ) {

            return;
        }

        for (
            AppointmentReminder reminder
            : activeReminders
        ) {

            if (
                reminder == null
            ) {

                continue;
            }

            OffsetDateTime reminderAppointmentStartAt =
                reminder.getAppointmentStartAt();

            /*
             * Un reminder sin snapshot sería una inconsistencia
             * después de V15.
             *
             * Lo cancelamos por seguridad si todavía está activo.
             */
            if (
                reminderAppointmentStartAt == null
            ) {

                reminder.cancel();

                continue;
            }

            /*
             * OffsetDateTime.isEqual compara el instante real,
             * independientemente del offset.
             */
            if (
                !reminderAppointmentStartAt.isEqual(
                    currentAppointmentStartAt
                )
            ) {

                reminder.cancel();
            }
        }

        appointmentReminderRepository.saveAll(
            activeReminders
        );
    }

    // =========================================================
    // CANCEL ACTIVE REMINDERS
    // =========================================================

    /**
     * Cancela todos los reminders activos de una Appointment.
     *
     * Se utiliza principalmente cuando:
     *
     * - la Appointment pasa a un estado terminal;
     * - el negocio deshabilita reminders;
     * - cancelBooking() cancela la cita.
     *
     * SENT permanece intacto.
     */
    @Transactional
    public int cancelActiveReminders(
        Long appointmentId
    ) {

        validateAppointmentId(
            appointmentId
        );

        return appointmentReminderRepository
            .cancelActiveRemindersByAppointmentId(
                appointmentId,
                ACTIVE_STATUSES,
                ReminderStatus.CANCELLED,
                OffsetDateTime.now()
            );
    }

    // =========================================================
    // FIND REMINDERS BY APPOINTMENT
    // =========================================================

    /**
     * Devuelve el histórico completo de reminders de una cita,
     * incluyendo ocurrencias pertenecientes a horarios anteriores.
     */
    @Transactional(readOnly = true)
    public List<AppointmentReminder> findAllByAppointmentId(
        Long appointmentId
    ) {

        validateAppointmentId(
            appointmentId
        );

        return appointmentReminderRepository
            .findAllByAppointmentIdOrderByAppointmentStartAtAscScheduledForAscIdAsc(
                appointmentId
            );
    }

    // =========================================================
    // FIND REMINDERS BY OCCURRENCE
    // =========================================================

    /**
     * Devuelve únicamente FIRST / SECOND correspondientes al
     * startAt indicado de una Appointment.
     */
    @Transactional(readOnly = true)
    public List<AppointmentReminder> findAllByAppointmentOccurrence(
        Long appointmentId,
        OffsetDateTime appointmentStartAt
    ) {

        validateAppointmentId(
            appointmentId
        );

        if (
            appointmentStartAt == null
        ) {

            throw new IllegalArgumentException(
                "appointmentStartAt es obligatorio."
            );
        }

        return appointmentReminderRepository
            .findAllByAppointmentIdAndAppointmentStartAtOrderByScheduledForAscIdAsc(
                appointmentId,
                appointmentStartAt
            );
    }

    // =========================================================
    // FIND DUE REMINDERS
    // =========================================================

    @Transactional(readOnly = true)
    public List<AppointmentReminder> findDueReminders(
        OffsetDateTime now
    ) {

        OffsetDateTime effectiveNow =
            now != null
                ? now
                : OffsetDateTime.now();

        return appointmentReminderRepository
            .findDueReminders(
                effectiveNow,
                List.of(
                    ReminderStatus.PENDING,
                    ReminderStatus.FAILED
                )
            );
    }

    // =========================================================
    // LOCK FOR PROCESSING
    // =========================================================

    /**
     * Obtiene un recordatorio bajo PESSIMISTIC_WRITE.
     *
     * El worker utiliza este método antes de intentar enviar
     * un mensaje para impedir procesamiento concurrente.
     */
    @Transactional
    public AppointmentReminder lockForProcessing(
        Long reminderId
    ) {

        validateReminderId(
            reminderId
        );

        return appointmentReminderRepository
            .findByIdForUpdate(
                reminderId
            )
            .orElseThrow(
                () ->
                    new ConflictException(
                        "No existe el recordatorio con id: "
                            + reminderId
                    )
            );
    }

    // =========================================================
    // MARK PROCESSING
    // =========================================================

    @Transactional
    public AppointmentReminder markProcessing(
        Long reminderId
    ) {

        AppointmentReminder reminder =
            lockForProcessing(
                reminderId
            );

        if (
            reminder.isSent()
                || reminder.isCancelled()
        ) {

            return reminder;
        }

        reminder.markProcessing(
            OffsetDateTime.now()
        );

        return appointmentReminderRepository
            .saveAndFlush(
                reminder
            );
    }

    // =========================================================
    // MARK SENT
    // =========================================================

    @Transactional
    public AppointmentReminder markSent(
        Long reminderId,
        String providerMessageId
    ) {

        AppointmentReminder reminder =
            lockForProcessing(
                reminderId
            );

        if (
            reminder.isSent()
        ) {

            return reminder;
        }

        if (
            reminder.isCancelled()
        ) {

            throw new ConflictException(
                "No se puede marcar como enviado "
                    + "un recordatorio cancelado."
            );
        }

        reminder.markSent(
            OffsetDateTime.now(),
            providerMessageId
        );

        return appointmentReminderRepository
            .saveAndFlush(
                reminder
            );
    }

    // =========================================================
    // MARK FAILED
    // =========================================================

    @Transactional
    public AppointmentReminder markFailed(
        Long reminderId,
        String error
    ) {

        AppointmentReminder reminder =
            lockForProcessing(
                reminderId
            );

        if (
            reminder.isSent()
                || reminder.isCancelled()
        ) {

            return reminder;
        }

        reminder.markFailed(
            OffsetDateTime.now(),
            normalizeError(
                error
            )
        );

        return appointmentReminderRepository
            .saveAndFlush(
                reminder
            );
    }

    // =========================================================
    // BUSINESS SETTINGS
    // =========================================================

    private BusinessSettings requireBusinessSettings(
        Long businessId
    ) {

        return businessSettingsRepository
            .findByBusinessId(
                businessId
            )
            .orElseThrow(
                () ->
                    new ConflictException(
                        "El negocio no tiene configuración operativa."
                    )
            );
    }

    private void validateBusinessSettings(
        BusinessSettings settings,
        Long expectedBusinessId
    ) {

        if (
            settings == null
                || settings.getId() == null
        ) {

            throw new IllegalStateException(
                "BusinessSettings no es válido."
            );
        }

        if (
            settings.getBusiness() == null
                || settings
                    .getBusiness()
                    .getId() == null
        ) {

            throw new IllegalStateException(
                "BusinessSettings no tiene un Business válido."
            );
        }

        if (
            !Objects.equals(
                settings
                    .getBusiness()
                    .getId(),
                expectedBusinessId
            )
        ) {

            throw new IllegalStateException(
                "BusinessSettings pertenece a otro negocio."
            );
        }

        if (
            settings.getFirstReminderMinutesBefore() <= 0
        ) {

            throw new IllegalStateException(
                "firstReminderMinutesBefore no es válido."
            );
        }

        if (
            settings.getSecondReminderMinutesBefore() <= 0
        ) {

            throw new IllegalStateException(
                "secondReminderMinutesBefore no es válido."
            );
        }

        if (
            settings.isSecondReminderEnabled()
                && !settings.isRemindersEnabled()
        ) {

            throw new IllegalStateException(
                "El segundo recordatorio no puede estar activo "
                    + "si los recordatorios generales "
                    + "están deshabilitados."
            );
        }

        if (
            settings.isSecondReminderEnabled()
                && settings
                    .getSecondReminderMinutesBefore()
                    >= settings
                        .getFirstReminderMinutesBefore()
        ) {

            throw new IllegalStateException(
                "El segundo recordatorio debe ejecutarse "
                    + "más cerca de la cita que el primero."
            );
        }
    }

    // =========================================================
    // APPOINTMENT VALIDATION
    // =========================================================

    private void validateAppointment(
        Appointment appointment
    ) {

        if (
            appointment == null
                || appointment.getId() == null
        ) {

            throw new IllegalArgumentException(
                "Appointment no es válida."
            );
        }

        if (
            appointment.getBusiness() == null
                || appointment
                    .getBusiness()
                    .getId() == null
        ) {

            throw new IllegalStateException(
                "Appointment no tiene un Business válido."
            );
        }

        if (
            appointment.getCustomer() == null
                || appointment
                    .getCustomer()
                    .getId() == null
        ) {

            throw new IllegalStateException(
                "Appointment no tiene un Customer válido."
            );
        }

        if (
            appointment.getStatus() == null
        ) {

            throw new IllegalStateException(
                "Appointment no tiene estado."
            );
        }

        if (
            appointment.getStartAt() == null
        ) {

            throw new IllegalStateException(
                "Appointment no tiene startAt."
            );
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    private boolean isReminderEligibleStatus(
        Appointment.AppointmentStatus status
    ) {

        return status
            == Appointment.AppointmentStatus.PENDING
            || status
            == Appointment.AppointmentStatus.CONFIRMED;
    }

    // =========================================================
    // CUSTOMER DESTINATION
    // =========================================================

    private String resolveEmailDestination(
        Customer customer
    ) {

        if (
            customer == null
        ) {

            return null;
        }

        String email =
            customer.getEmail();

        if (
            email == null
                || email.isBlank()
        ) {

            return null;
        }

        return email
            .trim()
            .toLowerCase();
    }

    // =========================================================
    // TIMEZONE
    // =========================================================

    private ZoneId resolveBusinessZone(
        Business business
    ) {

        if (
            business == null
        ) {

            throw new IllegalArgumentException(
                "Business es obligatorio."
            );
        }

        String timezone =
            business.getTimezone();

        if (
            timezone == null
                || timezone.isBlank()
        ) {

            throw new ConflictException(
                "El negocio no tiene una zona horaria configurada."
            );
        }

        try {

            return ZoneId.of(
                timezone.trim()
            );

        } catch (
            DateTimeException exception
        ) {

            throw new ConflictException(
                "La zona horaria del negocio no es válida."
            );
        }
    }

    // =========================================================
    // IDS
    // =========================================================

    private void validateAppointmentId(
        Long appointmentId
    ) {

        if (
            appointmentId == null
                || appointmentId <= 0
        ) {

            throw new IllegalArgumentException(
                "appointmentId no es válido."
            );
        }
    }

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
    // ERROR NORMALIZATION
    // =========================================================

    private String normalizeError(
        String error
    ) {

        if (
            error == null
                || error.isBlank()
        ) {

            return "Error de envío no especificado.";
        }

        String normalized =
            error.trim();

        if (
            normalized.length()
                <= MAX_ERROR_LENGTH
        ) {

            return normalized;
        }

        return normalized.substring(
            0,
            MAX_ERROR_LENGTH
        );
    }
}