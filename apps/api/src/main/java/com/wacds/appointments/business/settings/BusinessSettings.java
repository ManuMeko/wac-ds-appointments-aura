
package com.wacds.appointments.business.settings;

import com.wacds.appointments.business.Business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Configuracion operativa de reservas de un negocio.
 *
 * RESPONSABILIDADES
 *
 * - Antelacion minima para reservar.
 * - Horizonte maximo de reservas.
 * - Intervalos de disponibilidad.
 * - Buffers anteriores y posteriores.
 * - Confirmacion automatica.
 * - Cancelaciones online.
 * - Reprogramaciones online.
 * - Politicas de recordatorios.
 *
 * MULTITENANCY
 *
 * Cada Business tiene una unica configuracion.
 *
 * CONCURRENCIA
 *
 * @Version protege frente a modificaciones
 * concurrentes de la misma configuracion.
 *
 * El bloqueo de agenda de los profesionales
 * se gestiona independientemente en
 * AppointmentRepository.
 *
 * COMPATIBILIDAD
 *
 * Esta entidad conserva el contrato JPA
 * y los nombres de campos existentes.
 */
@Entity
@Table(
    name = "business_settings",
    indexes = {
        @Index(
            name = "idx_business_settings_business_id",
            columnList = "business_id",
            unique = true
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSettings {

    // =========================================================
    // DEFAULT VALUES
    // =========================================================

    /**
     * Antelacion minima para nuevas reservas.
     *
     * 120 minutos = 2 horas.
     */
    public static final int DEFAULT_MINIMUM_BOOKING_NOTICE_MINUTES =
        120;

    /**
     * Horizonte predeterminado de reservas.
     */
    public static final int DEFAULT_BOOKING_HORIZON_DAYS =
        60;

    /**
     * Separacion entre posibles inicios.
     *
     * No representa la duracion del servicio.
     */
    public static final int DEFAULT_SLOT_INTERVAL_MINUTES =
        15;

    public static final int DEFAULT_BUFFER_BEFORE_MINUTES =
        0;

    public static final int DEFAULT_BUFFER_AFTER_MINUTES =
        0;

    /**
     * 24 horas.
     */
    public static final int DEFAULT_CANCELLATION_NOTICE_MINUTES =
        1440;

    /**
     * 24 horas.
     */
    public static final int DEFAULT_RESCHEDULE_NOTICE_MINUTES =
        1440;

    /**
     * Primer recordatorio: 24 horas antes.
     */
    public static final int DEFAULT_FIRST_REMINDER_MINUTES_BEFORE =
        1440;

    /**
     * Segundo recordatorio: 2 horas antes.
     */
    public static final int DEFAULT_SECOND_REMINDER_MINUTES_BEFORE =
        120;

    // =========================================================
    // VALIDATION LIMITS
    // =========================================================

    private static final int MAX_NOTICE_MINUTES =
        525600;

    private static final int MIN_BOOKING_HORIZON_DAYS =
        1;

    private static final int MAX_BOOKING_HORIZON_DAYS =
        730;

    private static final int MIN_SLOT_INTERVAL_MINUTES =
        5;

    private static final int MAX_SLOT_INTERVAL_MINUTES =
        240;

    private static final int MAX_BUFFER_MINUTES =
        240;

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    // =========================================================
    // BUSINESS
    // =========================================================

    /**
     * Negocio propietario de la configuracion.
     *
     * Una configuracion por negocio.
     */
    @OneToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "business_id",
        nullable = false,
        unique = true
    )
    private Business business;

    // =========================================================
    // BOOKING WINDOW
    // =========================================================

    /**
     * Antelacion minima para crear una reserva.
     *
     * 0 permite reservar sin antelacion adicional,
     * siempre que el comienzo siga siendo futuro.
     */
    @Builder.Default
    @Column(
        name = "minimum_booking_notice_minutes",
        nullable = false
    )
    private int minimumBookingNoticeMinutes =
        DEFAULT_MINIMUM_BOOKING_NOTICE_MINUTES;

    /**
     * Numero maximo de dias hacia el futuro.
     */
    @Builder.Default
    @Column(
        name = "booking_horizon_days",
        nullable = false
    )
    private int bookingHorizonDays =
        DEFAULT_BOOKING_HORIZON_DAYS;

    // =========================================================
    // SLOT GENERATION
    // =========================================================

    /**
     * Intervalo base para generar horarios.
     *
     * Ejemplo: cada 15 minutos.
     */
    @Builder.Default
    @Column(
        name = "slot_interval_minutes",
        nullable = false
    )
    private int slotIntervalMinutes =
        DEFAULT_SLOT_INTERVAL_MINUTES;

    // =========================================================
    // DEFAULT BUFFERS
    // =========================================================

    /**
     * Margen antes de cada cita.
     */
    @Builder.Default
    @Column(
        name = "default_buffer_before_minutes",
        nullable = false
    )
    private int defaultBufferBeforeMinutes =
        DEFAULT_BUFFER_BEFORE_MINUTES;

    /**
     * Margen despues de cada cita.
     */
    @Builder.Default
    @Column(
        name = "default_buffer_after_minutes",
        nullable = false
    )
    private int defaultBufferAfterMinutes =
        DEFAULT_BUFFER_AFTER_MINUTES;

    // =========================================================
    // CONFIRMATION
    // =========================================================

    /**
     * true: cita inicialmente CONFIRMED.
     *
     * false: cita inicialmente PENDING.
     */
    @Builder.Default
    @Column(
        name = "auto_confirm_appointments",
        nullable = false
    )
    private boolean autoConfirmAppointments =
        true;

    // =========================================================
    // CUSTOMER CANCELLATION
    // =========================================================

    /**
     * Permite cancelar citas desde el enlace
     * de gestion del cliente.
     */
    @Builder.Default
    @Column(
        name = "online_cancellation_enabled",
        nullable = false
    )
    private boolean onlineCancellationEnabled =
        true;

    /**
     * Antelacion minima para cancelar online.
     */
    @Builder.Default
    @Column(
        name = "minimum_cancellation_notice_minutes",
        nullable = false
    )
    private int minimumCancellationNoticeMinutes =
        DEFAULT_CANCELLATION_NOTICE_MINUTES;

    // =========================================================
    // CUSTOMER RESCHEDULING
    // =========================================================

    /**
     * Permite reprogramaciones online.
     */
    @Builder.Default
    @Column(
        name = "online_rescheduling_enabled",
        nullable = false
    )
    private boolean onlineReschedulingEnabled =
        true;

    /**
     * Antelacion minima para reprogramar.
     */
    @Builder.Default
    @Column(
        name = "minimum_reschedule_notice_minutes",
        nullable = false
    )
    private int minimumRescheduleNoticeMinutes =
        DEFAULT_RESCHEDULE_NOTICE_MINUTES;

    // =========================================================
    // REMINDERS
    // =========================================================

    /**
     * Interruptor general de recordatorios.
     */
    @Builder.Default
    @Column(
        name = "reminders_enabled",
        nullable = false
    )
    private boolean remindersEnabled =
        true;

    /**
     * Minutos anteriores al comienzo de la cita
     * para el primer recordatorio.
     */
    @Builder.Default
    @Column(
        name = "first_reminder_minutes_before",
        nullable = false
    )
    private int firstReminderMinutesBefore =
        DEFAULT_FIRST_REMINDER_MINUTES_BEFORE;

    /**
     * Habilita un segundo recordatorio.
     */
    @Builder.Default
    @Column(
        name = "second_reminder_enabled",
        nullable = false
    )
    private boolean secondReminderEnabled =
        false;

    /**
     * Minutos anteriores a la cita
     * para el segundo recordatorio.
     */
    @Builder.Default
    @Column(
        name = "second_reminder_minutes_before",
        nullable = false
    )
    private int secondReminderMinutesBefore =
        DEFAULT_SECOND_REMINDER_MINUTES_BEFORE;

    // =========================================================
    // OPTIMISTIC LOCKING
    // =========================================================

    /**
     * Control de version utilizado por JPA
     * para detectar actualizaciones concurrentes.
     */
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    // =========================================================
    // AUDIT
    // =========================================================

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private OffsetDateTime updatedAt;

    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    /**
     * Valida la configuracion e inicializa
     * las fechas de auditoria.
     */
    @PrePersist
    void prePersist() {

        validate();

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (
            updatedAt == null
                || updatedAt.isBefore(createdAt)
        ) {
            updatedAt = createdAt;
        }

        if (now.isAfter(updatedAt)) {
            updatedAt = now;
        }
    }

    /**
     * Valida antes de actualizar.
     *
     * Evita retrocesos en updatedAt.
     */
    @PreUpdate
    void preUpdate() {

        validate();

        OffsetDateTime now = OffsetDateTime.now();

        if (
            createdAt != null
                && now.isBefore(createdAt)
        ) {
            updatedAt = createdAt;
            return;
        }

        if (
            updatedAt == null
                || now.isAfter(updatedAt)
        ) {
            updatedAt = now;
        }
    }

    // =========================================================
    // DOMAIN VALIDATION
    // =========================================================

    /**
     * Validacion global antes de persistir.
     */
    private void validate() {

        validateBusiness();

        validateBookingWindow();

        validateSlotInterval();

        validateBuffers();

        validateCancellationPolicy();

        validateReschedulingPolicy();

        validateReminderPolicy();
    }

    // =========================================================
    // BUSINESS VALIDATION
    // =========================================================

    private void validateBusiness() {

        if (business == null) {
            throw new IllegalStateException(
                "BusinessSettings debe pertenecer a un Business."
            );
        }
    }

    // =========================================================
    // BOOKING WINDOW VALIDATION
    // =========================================================

    private void validateBookingWindow() {

        validateBookingWindowValues(
            minimumBookingNoticeMinutes,
            bookingHorizonDays
        );
    }

    private void validateBookingWindowValues(
        int noticeMinutes,
        int horizonDays
    ) {

        validateRange(
            noticeMinutes,
            0,
            MAX_NOTICE_MINUTES,
            "minimumBookingNoticeMinutes"
        );

        validateRange(
            horizonDays,
            MIN_BOOKING_HORIZON_DAYS,
            MAX_BOOKING_HORIZON_DAYS,
            "bookingHorizonDays"
        );
    }

    // =========================================================
    // SLOT INTERVAL VALIDATION
    // =========================================================

    private void validateSlotInterval() {

        validateSlotIntervalValue(
            slotIntervalMinutes
        );
    }

    private void validateSlotIntervalValue(
        int minutes
    ) {

        validateRange(
            minutes,
            MIN_SLOT_INTERVAL_MINUTES,
            MAX_SLOT_INTERVAL_MINUTES,
            "slotIntervalMinutes"
        );
    }

    // =========================================================
    // BUFFERS VALIDATION
    // =========================================================

    private void validateBuffers() {

        validateBufferValues(
            defaultBufferBeforeMinutes,
            defaultBufferAfterMinutes
        );
    }

    private void validateBufferValues(
        int beforeMinutes,
        int afterMinutes
    ) {

        validateRange(
            beforeMinutes,
            0,
            MAX_BUFFER_MINUTES,
            "defaultBufferBeforeMinutes"
        );

        validateRange(
            afterMinutes,
            0,
            MAX_BUFFER_MINUTES,
            "defaultBufferAfterMinutes"
        );
    }

    // =========================================================
    // CANCELLATION VALIDATION
    // =========================================================

    private void validateCancellationPolicy() {

        validateCancellationNoticeValue(
            minimumCancellationNoticeMinutes
        );
    }

    private void validateCancellationNoticeValue(
        int minutes
    ) {

        validateRange(
            minutes,
            0,
            MAX_NOTICE_MINUTES,
            "minimumCancellationNoticeMinutes"
        );
    }

    // =========================================================
    // RESCHEDULING VALIDATION
    // =========================================================

    private void validateReschedulingPolicy() {

        validateReschedulingNoticeValue(
            minimumRescheduleNoticeMinutes
        );
    }

    private void validateReschedulingNoticeValue(
        int minutes
    ) {

        validateRange(
            minutes,
            0,
            MAX_NOTICE_MINUTES,
            "minimumRescheduleNoticeMinutes"
        );
    }

    // =========================================================
    // REMINDER VALIDATION
    // =========================================================

    private void validateReminderPolicy() {

        validateReminderValues(
            remindersEnabled,
            firstReminderMinutesBefore,
            secondReminderEnabled,
            secondReminderMinutesBefore
        );
    }

    private void validateReminderValues(
        boolean remindersActive,
        int firstMinutes,
        boolean secondActive,
        int secondMinutes
    ) {

        validateRange(
            firstMinutes,
            1,
            MAX_NOTICE_MINUTES,
            "firstReminderMinutesBefore"
        );

        validateRange(
            secondMinutes,
            1,
            MAX_NOTICE_MINUTES,
            "secondReminderMinutesBefore"
        );

        if (
            secondActive
                && !remindersActive
        ) {
            throw new IllegalStateException(
                "No puede habilitarse el segundo recordatorio "
                    + "si los recordatorios generales "
                    + "estan deshabilitados."
            );
        }

        if (
            secondActive
                && secondMinutes >= firstMinutes
        ) {
            throw new IllegalStateException(
                "El segundo recordatorio debe ejecutarse "
                    + "mas cerca de la cita que el primero."
            );
        }
    }

    // =========================================================
    // DOMAIN HELPERS - BOOKING
    // =========================================================

    /**
     * Cambia la antelacion minima.
     *
     * Valida antes de modificar la entidad.
     */
    public void changeMinimumBookingNoticeMinutes(
        int minutes
    ) {

        validateBookingWindowValues(
            minutes,
            bookingHorizonDays
        );

        this.minimumBookingNoticeMinutes = minutes;
    }

    /**
     * Cambia el horizonte de reservas.
     */
    public void changeBookingHorizonDays(
        int days
    ) {

        validateBookingWindowValues(
            minimumBookingNoticeMinutes,
            days
        );

        this.bookingHorizonDays = days;
    }

    /**
     * Cambia el intervalo entre inicios posibles.
     */
    public void changeSlotIntervalMinutes(
        int minutes
    ) {

        validateSlotIntervalValue(minutes);

        this.slotIntervalMinutes = minutes;
    }

    // =========================================================
    // DOMAIN HELPERS - BUFFERS
    // =========================================================

    /**
     * Modifica ambos buffers de forma consistente.
     */
    public void changeDefaultBuffers(
        int beforeMinutes,
        int afterMinutes
    ) {

        validateBufferValues(
            beforeMinutes,
            afterMinutes
        );

        this.defaultBufferBeforeMinutes =
            beforeMinutes;

        this.defaultBufferAfterMinutes =
            afterMinutes;
    }

    // =========================================================
    // DOMAIN HELPERS - CONFIRMATION
    // =========================================================

    public void enableAutoConfirmation() {

        this.autoConfirmAppointments = true;
    }

    public void disableAutoConfirmation() {

        this.autoConfirmAppointments = false;
    }

    public void changeAutoConfirmation(
        boolean enabled
    ) {

        this.autoConfirmAppointments = enabled;
    }

    // =========================================================
    // DOMAIN HELPERS - CANCELLATION
    // =========================================================

    public void enableOnlineCancellation() {

        this.onlineCancellationEnabled = true;
    }

    public void disableOnlineCancellation() {

        this.onlineCancellationEnabled = false;
    }

    public void changeOnlineCancellation(
        boolean enabled
    ) {

        this.onlineCancellationEnabled = enabled;
    }

    public void changeMinimumCancellationNoticeMinutes(
        int minutes
    ) {

        validateCancellationNoticeValue(minutes);

        this.minimumCancellationNoticeMinutes =
            minutes;
    }

    // =========================================================
    // DOMAIN HELPERS - RESCHEDULING
    // =========================================================

    public void enableOnlineRescheduling() {

        this.onlineReschedulingEnabled = true;
    }

    public void disableOnlineRescheduling() {

        this.onlineReschedulingEnabled = false;
    }

    public void changeOnlineRescheduling(
        boolean enabled
    ) {

        this.onlineReschedulingEnabled = enabled;
    }

    public void changeMinimumRescheduleNoticeMinutes(
        int minutes
    ) {

        validateReschedulingNoticeValue(minutes);

        this.minimumRescheduleNoticeMinutes =
            minutes;
    }

    // =========================================================
    // DOMAIN HELPERS - REMINDERS
    // =========================================================

    public void enableReminders() {

        this.remindersEnabled = true;
    }

    /**
     * Desactiva recordatorios generales
     * y el segundo recordatorio.
     */
    public void disableReminders() {

        this.remindersEnabled = false;
        this.secondReminderEnabled = false;
    }

    public void changeRemindersEnabled(
        boolean enabled
    ) {

        if (enabled) {
            enableReminders();
        } else {
            disableReminders();
        }
    }

    /**
     * Modifica el momento del primer recordatorio.
     *
     * Si el segundo esta habilitado, comprueba
     * que siga siendo posterior al primero.
     */
    public void changeFirstReminderMinutesBefore(
        int minutes
    ) {

        validateReminderValues(
            remindersEnabled,
            minutes,
            secondReminderEnabled,
            secondReminderMinutesBefore
        );

        this.firstReminderMinutesBefore = minutes;
    }

    /**
     * Habilita el segundo recordatorio
     * solo si la politica resultante es valida.
     */
    public void enableSecondReminder() {

        validateReminderValues(
            remindersEnabled,
            firstReminderMinutesBefore,
            true,
            secondReminderMinutesBefore
        );

        this.secondReminderEnabled = true;
    }

    public void disableSecondReminder() {

        this.secondReminderEnabled = false;
    }

    public void changeSecondReminderEnabled(
        boolean enabled
    ) {

        if (enabled) {
            enableSecondReminder();
        } else {
            disableSecondReminder();
        }
    }

    /**
     * Modifica el segundo recordatorio.
     */
    public void changeSecondReminderMinutesBefore(
        int minutes
    ) {

        validateReminderValues(
            remindersEnabled,
            firstReminderMinutesBefore,
            secondReminderEnabled,
            minutes
        );

        this.secondReminderMinutesBefore = minutes;
    }

    // =========================================================
    // DOMAIN QUERIES
    // =========================================================

    /**
     * Indica si se permiten reservas sin
     * antelacion minima adicional.
     */
    public boolean allowsImmediateBooking() {

        return minimumBookingNoticeMinutes == 0;
    }

    public boolean canCustomerCancelOnline() {

        return onlineCancellationEnabled;
    }

    public boolean canCustomerRescheduleOnline() {

        return onlineReschedulingEnabled;
    }

    public boolean usesAutomaticConfirmation() {

        return autoConfirmAppointments;
    }

    public boolean hasSecondReminder() {

        return remindersEnabled
            && secondReminderEnabled;
    }

    // =========================================================
    // INTERNAL VALIDATION HELPERS
    // =========================================================

    /**
     * Valida un valor entero dentro de un rango.
     *
     * Incluye ambos extremos.
     */
    private void validateRange(
        int value,
        int minimum,
        int maximum,
        String fieldName
    ) {

        if (value < minimum) {
            throw new IllegalStateException(
                fieldName
                    + " no puede ser inferior a "
                    + minimum
                    + "."
            );
        }

        if (value > maximum) {
            throw new IllegalStateException(
                fieldName
                    + " no puede superar "
                    + maximum
                    + "."
            );
        }
    }
}
