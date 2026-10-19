package com.wacds.appointments.reminder;

import com.wacds.appointments.reminder.AppointmentReminder.ReminderStatus;
import com.wacds.appointments.reminder.AppointmentReminder.ReminderType;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentReminderRepository
    extends JpaRepository<AppointmentReminder, Long> {

    // =========================================================
    // OCCURRENCE-AWARE LOOKUPS
    // =========================================================

    /**
     * Recupera una ocurrencia concreta de reminder.
     *
     * Desde V15 ya NO es correcto identificar un reminder
     * únicamente mediante:
     *
     * appointmentId + reminderType
     *
     * porque una misma Appointment puede conservar varios
     * FIRST / SECOND históricos después de reprogramaciones.
     *
     * La identidad lógica actual es:
     *
     * appointmentId
     * + reminderType
     * + appointmentStartAt
     */
    Optional<AppointmentReminder>
        findByAppointmentIdAndReminderTypeAndAppointmentStartAt(
            Long appointmentId,
            ReminderType reminderType,
            OffsetDateTime appointmentStartAt
        );

    /**
     * Comprueba si ya existe exactamente la ocurrencia de reminder
     * correspondiente al horario indicado.
     */
    boolean
        existsByAppointmentIdAndReminderTypeAndAppointmentStartAt(
            Long appointmentId,
            ReminderType reminderType,
            OffsetDateTime appointmentStartAt
        );

    // =========================================================
    // APPOINTMENT HISTORY
    // =========================================================

    /**
     * Recupera el histórico completo de reminders de una cita.
     *
     * Primero se ordena por el startAt de la ocurrencia y después
     * por el instante programado de envío.
     */
    List<AppointmentReminder>
        findAllByAppointmentIdOrderByAppointmentStartAtAscScheduledForAscIdAsc(
            Long appointmentId
        );

    /**
     * Recupera todos los reminders correspondientes a una
     * ocurrencia concreta de la Appointment.
     *
     * Normalmente podrá contener:
     *
     * FIRST
     * SECOND
     *
     * dependiendo de la configuración del negocio.
     */
    List<AppointmentReminder>
        findAllByAppointmentIdAndAppointmentStartAtOrderByScheduledForAscIdAsc(
            Long appointmentId,
            OffsetDateTime appointmentStartAt
        );

    /**
     * Recupera el histórico de un tipo concreto de reminder
     * para una Appointment.
     *
     * Ejemplo:
     *
     * todos los FIRST generados a lo largo de varias
     * reprogramaciones.
     */
    List<AppointmentReminder>
        findAllByAppointmentIdAndReminderTypeOrderByAppointmentStartAtAscIdAsc(
            Long appointmentId,
            ReminderType reminderType
        );

    // =========================================================
    // DUE REMINDERS
    // =========================================================

    /**
     * Recupera recordatorios pendientes o fallidos cuyo
     * momento programado ya ha llegado.
     *
     * El worker será responsable de limitar el tamaño efectivo
     * del lote que procesa.
     */
    @Query(
        """
        select reminder
        from AppointmentReminder reminder
        where reminder.scheduledFor <= :now
          and reminder.status in :statuses
        order by reminder.scheduledFor asc,
                 reminder.id asc
        """
    )
    List<AppointmentReminder> findDueReminders(
        @Param("now")
        OffsetDateTime now,

        @Param("statuses")
        List<ReminderStatus> statuses
    );

    // =========================================================
    // LOCKED PROCESSING
    // =========================================================

    /**
     * Recupera un recordatorio aplicando bloqueo pesimista.
     *
     * Este método es utilizado por el worker inmediatamente antes
     * de procesar el reminder.
     *
     * Además del propio AppointmentReminder, cargamos de forma
     * explícita el contexto necesario para construir el email
     * transaccional fuera de esta transacción:
     *
     * - Appointment;
     * - Business;
     * - Customer;
     * - Professional;
     * - Service.
     *
     * Esto evita LazyInitializationException y evita que la
     * plantilla tenga que recurrir a fallbacks como:
     *
     * - "Tu centro";
     * - "Tu cita";
     * - "Nuestro equipo".
     *
     * IMPORTANTE:
     *
     * mantenemos PESSIMISTIC_WRITE para impedir que dos workers
     * intenten procesar simultáneamente el mismo reminder.
     */
    @Lock(
        LockModeType.PESSIMISTIC_WRITE
    )
    @Query(
        """
        select reminder
        from AppointmentReminder reminder
        join fetch reminder.appointment appointment
        join fetch appointment.business business
        join fetch appointment.customer customer
        join fetch appointment.professional professional
        join fetch appointment.service service
        where reminder.id = :reminderId
        """
    )
    Optional<AppointmentReminder> findByIdForUpdate(
        @Param("reminderId")
        Long reminderId
    );

    // =========================================================
    // ACTIVE REMINDERS BY APPOINTMENT
    // =========================================================

    /**
     * Recupera todos los reminders activos de una Appointment,
     * independientemente de la ocurrencia temporal a la que
     * pertenezcan.
     *
     * Es útil especialmente para cancelBooking(), donde la
     * cancelación de la Appointment debe dejar sin efecto todos
     * los reminders todavía activos.
     */
    @Query(
        """
        select reminder
        from AppointmentReminder reminder
        where reminder.appointment.id = :appointmentId
          and reminder.status in :statuses
        order by reminder.appointmentStartAt asc,
                 reminder.scheduledFor asc,
                 reminder.id asc
        """
    )
    List<AppointmentReminder> findAllByAppointmentIdAndStatuses(
        @Param("appointmentId")
        Long appointmentId,

        @Param("statuses")
        List<ReminderStatus> statuses
    );

    /**
     * Recupera reminders activos únicamente para una ocurrencia
     * concreta de la Appointment.
     *
     * Esto permite operar sobre el horario anterior durante una
     * reprogramación sin afectar al histórico de otras ocurrencias.
     */
    @Query(
        """
        select reminder
        from AppointmentReminder reminder
        where reminder.appointment.id = :appointmentId
          and reminder.appointmentStartAt = :appointmentStartAt
          and reminder.status in :statuses
        order by reminder.scheduledFor asc,
                 reminder.id asc
        """
    )
    List<AppointmentReminder>
        findAllByAppointmentIdAndAppointmentStartAtAndStatuses(
            @Param("appointmentId")
            Long appointmentId,

            @Param("appointmentStartAt")
            OffsetDateTime appointmentStartAt,

            @Param("statuses")
            List<ReminderStatus> statuses
        );

    // =========================================================
    // CANCEL ALL ACTIVE REMINDERS BY APPOINTMENT
    // =========================================================

    /**
     * Cancela todos los recordatorios activos de una Appointment.
     *
     * Se utiliza cuando la propia cita se cancela.
     *
     * SENT no debe incluirse dentro de activeStatuses, por lo
     * que el histórico enviado permanece intacto.
     */
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query(
        """
        update AppointmentReminder reminder
        set reminder.status = :cancelledStatus,
            reminder.updatedAt = :now
        where reminder.appointment.id = :appointmentId
          and reminder.status in :activeStatuses
        """
    )
    int cancelActiveRemindersByAppointmentId(
        @Param("appointmentId")
        Long appointmentId,

        @Param("activeStatuses")
        List<ReminderStatus> activeStatuses,

        @Param("cancelledStatus")
        ReminderStatus cancelledStatus,

        @Param("now")
        OffsetDateTime now
    );

    // =========================================================
    // CANCEL ACTIVE REMINDERS BY OCCURRENCE
    // =========================================================

    /**
     * Cancela únicamente los reminders activos pertenecientes
     * al horario indicado de una Appointment.
     *
     * Está preparado para el flujo de reprogramación:
     *
     * horario anterior
     * -> cancelar reminders todavía activos
     * -> conservar SENT como histórico
     * -> crear reminders para el nuevo horario
     */
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query(
        """
        update AppointmentReminder reminder
        set reminder.status = :cancelledStatus,
            reminder.updatedAt = :now
        where reminder.appointment.id = :appointmentId
          and reminder.appointmentStartAt = :appointmentStartAt
          and reminder.status in :activeStatuses
        """
    )
    int cancelActiveRemindersByAppointmentIdAndAppointmentStartAt(
        @Param("appointmentId")
        Long appointmentId,

        @Param("appointmentStartAt")
        OffsetDateTime appointmentStartAt,

        @Param("activeStatuses")
        List<ReminderStatus> activeStatuses,

        @Param("cancelledStatus")
        ReminderStatus cancelledStatus,

        @Param("now")
        OffsetDateTime now
    );

    // =========================================================
    // DELETE NON-SENT REMINDERS BY OCCURRENCE
    // =========================================================

    /**
     * Elimina recordatorios no enviados pertenecientes a una
     * ocurrencia temporal concreta.
     *
     * IMPORTANTE:
     *
     * Este método se conserva como herramienta técnica, pero el
     * flujo principal de reprogramación debería priorizar CANCELLED
     * sobre DELETE cuando queramos mantener trazabilidad completa.
     *
     * Nunca elimina SENT.
     */
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query(
        """
        delete from AppointmentReminder reminder
        where reminder.appointment.id = :appointmentId
          and reminder.appointmentStartAt = :appointmentStartAt
          and reminder.status <> :sentStatus
        """
    )
    int deleteAllNonSentByAppointmentIdAndAppointmentStartAt(
        @Param("appointmentId")
        Long appointmentId,

        @Param("appointmentStartAt")
        OffsetDateTime appointmentStartAt,

        @Param("sentStatus")
        ReminderStatus sentStatus
    );

    // =========================================================
    // FAILED REMINDERS
    // =========================================================

    /**
     * Recupera reminders fallidos susceptibles de reintento.
     *
     * El límite máximo de intentos se aplica antes de que vuelvan
     * a entrar en procesamiento.
     */
    @Query(
        """
        select reminder
        from AppointmentReminder reminder
        where reminder.status = :failedStatus
          and reminder.attemptCount < :maxAttempts
          and reminder.scheduledFor <= :now
        order by reminder.lastAttemptAt asc nulls first,
                 reminder.scheduledFor asc,
                 reminder.id asc
        """
    )
    List<AppointmentReminder> findRetryableFailedReminders(
        @Param("failedStatus")
        ReminderStatus failedStatus,

        @Param("maxAttempts")
        int maxAttempts,

        @Param("now")
        OffsetDateTime now
    );

    // =========================================================
    // HOUSEKEEPING
    // =========================================================

    /**
     * Recupera reminders que permanecen en PROCESSING durante
     * demasiado tiempo.
     *
     * Permite recuperar trabajos interrumpidos por una caída del
     * proceso o cualquier otra condición anómala.
     */
    @Query(
        """
        select reminder
        from AppointmentReminder reminder
        where reminder.status = :processingStatus
          and reminder.lastAttemptAt < :before
        order by reminder.lastAttemptAt asc,
                 reminder.id asc
        """
    )
    List<AppointmentReminder> findStaleProcessingReminders(
        @Param("processingStatus")
        ReminderStatus processingStatus,

        @Param("before")
        OffsetDateTime before
    );
}