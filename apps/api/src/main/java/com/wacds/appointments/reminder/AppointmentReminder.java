
package com.wacds.appointments.reminder;

import com.wacds.appointments.appointment.Appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Recordatorio asociado a una Appointment.
 *
 * V15 - OCCURRENCE SNAPSHOT
 *
 * La identidad logica de cada recordatorio es:
 *
 * appointment_id
 * + reminder_type
 * + appointment_start_at
 *
 * Esto permite conservar recordatorios historicos
 * cuando una cita cambia de fecha.
 *
 * Ejemplo:
 *
 * FIRST - 11:20 - CANCELLED
 * FIRST - 18:00 - PENDING
 *
 * RESPONSABILIDADES:
 *
 * - Identificar la cita y su ocurrencia.
 * - Definir el tipo de recordatorio.
 * - Definir el canal de entrega.
 * - Establecer la programacion.
 * - Mantener el estado de procesamiento.
 * - Registrar intentos y resultados.
 * - Conservar informacion de auditoria.
 *
 * La entrega real corresponde a:
 *
 * ReminderDeliveryService.
 *
 * La coordinacion y las transacciones
 * corresponden a los servicios y al worker.
 *
 * COMPATIBILIDAD:
 *
 * - Mantiene todas las columnas existentes.
 * - Mantiene los enums.
 * - Mantiene los metodos publicos.
 * - Mantiene la restriccion unica V15.
 * - No requiere migraciones Flyway.
 */
@Entity
@Table(
    name = "appointment_reminders",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_appointment_reminders_appointment_type_start",
            columnNames = {
                "appointment_id",
                "reminder_type",
                "appointment_start_at"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_appointment_reminders_appointment_id",
            columnList = "appointment_id"
        ),
        @Index(
            name = "idx_appointment_reminders_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_appointment_reminders_scheduled_for",
            columnList = "scheduled_for"
        ),
        @Index(
            name = "idx_appointment_reminders_due",
            columnList = "status,scheduled_for"
        ),
        @Index(
            name = "idx_appointment_reminders_appointment_start",
            columnList = "appointment_id,appointment_start_at"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReminder {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_DESTINATION_LENGTH = 320;

    private static final int MAX_PROVIDER_MESSAGE_ID_LENGTH = 255;

    private static final int MAX_LAST_ERROR_LENGTH = 2000;

    // =========================================================
    // ENUMS
    // =========================================================

    public enum ReminderType {
        FIRST,
        SECOND
    }

    public enum ReminderStatus {
        PENDING,
        PROCESSING,
        SENT,
        FAILED,
        CANCELLED
    }

    public enum ReminderChannel {
        EMAIL,
        SMS,
        WHATSAPP
    }

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    // =========================================================
    // APPOINTMENT
    // =========================================================

    /**
     * Cita propietaria del recordatorio.
     *
     * No se configura eliminacion en cascada
     * desde esta entidad.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "appointment_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_appointment_reminders_appointment"
        )
    )
    private Appointment appointment;

    // =========================================================
    // OCCURRENCE SNAPSHOT - V15
    // =========================================================

    /**
     * Fecha original de la ocurrencia
     * para la que se genero el recordatorio.
     *
     * No tiene por que coincidir con el
     * startAt actual de Appointment.
     *
     * No debe modificarse al reprogramar.
     */
    @Column(
        name = "appointment_start_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime appointmentStartAt;

    // =========================================================
    // REMINDER TYPE
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
        name = "reminder_type",
        nullable = false,
        length = 20
    )
    private ReminderType reminderType;

    // =========================================================
    // CHANNEL
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
        name = "channel",
        nullable = false,
        length = 20
    )
    private ReminderChannel channel;

    // =========================================================
    // STATUS
    // =========================================================

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    private ReminderStatus status;

    // =========================================================
    // SCHEDULING
    // =========================================================

    /**
     * Momento en el que el recordatorio
     * debe entrar en procesamiento.
     */
    @Column(
        name = "scheduled_for",
        nullable = false
    )
    private OffsetDateTime scheduledFor;

    /**
     * Momento real de entrega.
     */
    @Column(
        name = "sent_at"
    )
    private OffsetDateTime sentAt;

    /**
     * Ultimo intento de procesamiento.
     */
    @Column(
        name = "last_attempt_at"
    )
    private OffsetDateTime lastAttemptAt;

    // =========================================================
    // RETRY
    // =========================================================

    /**
     * Numero de intentos realizados.
     *
     * El limite de reintentos corresponde
     * al AppointmentReminderWorker.
     */
    @Builder.Default
    @Column(
        name = "attempt_count",
        nullable = false
    )
    private int attemptCount = 0;

    // =========================================================
    // DELIVERY INFORMATION
    // =========================================================

    /**
     * Email o telefono de destino.
     */
    @Column(
        name = "destination",
        length = MAX_DESTINATION_LENGTH
    )
    private String destination;

    /**
     * Identificador devuelto por el proveedor.
     */
    @Column(
        name = "provider_message_id",
        length = MAX_PROVIDER_MESSAGE_ID_LENGTH
    )
    private String providerMessageId;

    /**
     * Ultimo error registrado.
     *
     * No debe contener credenciales,
     * contrasenas ni tokens.
     */
    @Column(
        name = "last_error",
        length = MAX_LAST_ERROR_LENGTH
    )
    private String lastError;

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
    // JPA - CREATE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        OffsetDateTime now = OffsetDateTime.now();

        /*
         * Compatibilidad con los caminos
         * de creacion existentes.
         */
        if (
            appointmentStartAt == null
                && appointment != null
                && appointment.getStartAt() != null
        ) {
            appointmentStartAt =
                appointment.getStartAt();
        }

        if (status == null) {
            status = ReminderStatus.PENDING;
        }

        if (channel == null) {
            channel = ReminderChannel.EMAIL;
        }

        /*
         * Conservamos la normalizacion existente
         * del contador negativo durante la creacion.
         */
        if (attemptCount < 0) {
            attemptCount = 0;
        }

        normalizeDeliveryFields();

        validateEntityState();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now.isAfter(createdAt)
            ? now
            : createdAt;
    }

    // =========================================================
    // JPA - UPDATE
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        /*
         * appointmentStartAt es un snapshot
         * historico y no se modifica aqui.
         */
        normalizeDeliveryFields();

        validateEntityState();

        OffsetDateTime now = OffsetDateTime.now();

        updatedAt = now.isAfter(createdAt)
            ? now
            : createdAt;
    }

    // =========================================================
    // ENTITY VALIDATION
    // =========================================================

    private void validateEntityState() {

        validateAppointment();

        validateOccurrence();

        validateRequiredFields();

        validateAttempts();

        validateDeliveryTimestamps();
    }

    // =========================================================
    // APPOINTMENT VALIDATION
    // =========================================================

    private void validateAppointment() {

        if (appointment == null) {
            throw new IllegalStateException(
                "El recordatorio debe estar "
                    + "asociado a una cita."
            );
        }

        if (
            appointment.getId() == null
                || appointment.getId() <= 0
        ) {
            throw new IllegalStateException(
                "La cita del recordatorio "
                    + "debe estar persistida."
            );
        }
    }

    // =========================================================
    // OCCURRENCE VALIDATION
    // =========================================================

    private void validateOccurrence() {

        if (appointmentStartAt == null) {
            throw new IllegalStateException(
                "appointmentStartAt es obligatorio "
                    + "para identificar la ocurrencia."
            );
        }

        /*
         * No comparamos con el startAt actual:
         * una reprogramacion puede cambiarlos.
         */
    }

    // =========================================================
    // REQUIRED FIELD VALIDATION
    // =========================================================

    private void validateRequiredFields() {

        if (reminderType == null) {
            throw new IllegalStateException(
                "reminderType es obligatorio."
            );
        }

        if (channel == null) {
            throw new IllegalStateException(
                "channel es obligatorio."
            );
        }

        if (status == null) {
            throw new IllegalStateException(
                "status es obligatorio."
            );
        }

        if (scheduledFor == null) {
            throw new IllegalStateException(
                "scheduledFor es obligatorio."
            );
        }
    }

    // =========================================================
    // ATTEMPTS VALIDATION
    // =========================================================

    private void validateAttempts() {

        if (attemptCount < 0) {
            throw new IllegalStateException(
                "attemptCount no puede ser negativo."
            );
        }
    }

    // =========================================================
    // DELIVERY TIMESTAMP VALIDATION
    // =========================================================

    private void validateDeliveryTimestamps() {

        /*
         * SENT debe registrar un instante
         * de entrega real.
         */
        if (
            status == ReminderStatus.SENT
                && sentAt == null
        ) {
            throw new IllegalStateException(
                "Un recordatorio SENT debe "
                    + "tener sentAt."
            );
        }

        /*
         * No eliminamos sentAt en actualizaciones.
         *
         * Conservamos informacion historica
         * que pudiera haberse registrado.
         */
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private void normalizeDeliveryFields() {

        destination = normalizeNullable(
            destination,
            MAX_DESTINATION_LENGTH
        );

        providerMessageId = normalizeNullable(
            providerMessageId,
            MAX_PROVIDER_MESSAGE_ID_LENGTH
        );

        lastError = normalizeNullable(
            lastError,
            MAX_LAST_ERROR_LENGTH
        );
    }

    // =========================================================
    // STATUS HELPERS
    // =========================================================

    public boolean isPending() {
        return status == ReminderStatus.PENDING;
    }

    public boolean isProcessing() {
        return status == ReminderStatus.PROCESSING;
    }

    public boolean isSent() {
        return status == ReminderStatus.SENT;
    }

    public boolean isFailed() {
        return status == ReminderStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == ReminderStatus.CANCELLED;
    }

    /**
     * Estados terminales.
     */
    public boolean isTerminal() {

        return isSent() || isCancelled();
    }

    // =========================================================
    // DUE
    // =========================================================

    /**
     * Determina si el recordatorio esta vencido
     * y puede ser candidato para el worker.
     *
     * No sustituye el bloqueo ni las
     * validaciones del servicio.
     */
    public boolean isDue(
        OffsetDateTime now
    ) {

        if (
            now == null
                || scheduledFor == null
        ) {
            return false;
        }

        return (
            status == ReminderStatus.PENDING
                || status == ReminderStatus.FAILED
        )
            && !scheduledFor.isAfter(now);
    }

    // =========================================================
    // PROCESSING
    // =========================================================

    /**
     * Inicia un intento de procesamiento.
     *
     * El contador se incrementa aqui.
     *
     * No se incrementa nuevamente
     * en markSent o markFailed.
     */
    public void markProcessing(
        OffsetDateTime now
    ) {

        OffsetDateTime effectiveNow =
            now != null
                ? now
                : OffsetDateTime.now();

        status = ReminderStatus.PROCESSING;

        lastAttemptAt = effectiveNow;

        attemptCount++;
    }

    // =========================================================
    // SENT
    // =========================================================

    /**
     * Marca la entrega correcta.
     */
    public void markSent(
        OffsetDateTime now,
        String providerMessageId
    ) {

        OffsetDateTime effectiveNow =
            now != null
                ? now
                : OffsetDateTime.now();

        status = ReminderStatus.SENT;

        sentAt = effectiveNow;

        lastAttemptAt = effectiveNow;

        this.providerMessageId =
            normalizeNullable(
                providerMessageId,
                MAX_PROVIDER_MESSAGE_ID_LENGTH
            );

        /*
         * Una entrega correcta elimina
         * los errores de intentos anteriores.
         */
        lastError = null;
    }

    // =========================================================
    // FAILED
    // =========================================================

    /**
     * Marca el ultimo intento como fallido.
     *
     * No incrementa attemptCount.
     */
    public void markFailed(
        OffsetDateTime now,
        String error
    ) {

        OffsetDateTime effectiveNow =
            now != null
                ? now
                : OffsetDateTime.now();

        status = ReminderStatus.FAILED;

        lastAttemptAt = effectiveNow;

        lastError = normalizeNullable(
            error,
            MAX_LAST_ERROR_LENGTH
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    /**
     * Cancela un recordatorio no entregado.
     *
     * Los recordatorios SENT se conservan
     * como historico.
     */
    public void cancel() {

        if (status == ReminderStatus.SENT) {
            return;
        }

        status = ReminderStatus.CANCELLED;
    }

    // =========================================================
    // DELIVERY HELPERS
    // =========================================================

    public boolean hasDestination() {

        return destination != null
            && !destination.isBlank();
    }

    public boolean hasProviderMessageId() {

        return providerMessageId != null
            && !providerMessageId.isBlank();
    }

    public boolean hasLastError() {

        return lastError != null
            && !lastError.isBlank();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Normaliza texto opcional.
     *
     * - null permanece null.
     * - Elimina espacios exteriores.
     * - Convierte cadenas vacias en null.
     * - Limita a la longitud de columna.
     *
     * Se conserva el truncamiento existente
     * por compatibilidad con los servicios.
     */
    private String normalizeNullable(
        String value,
        int maxLength
    ) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > maxLength) {
            return normalized.substring(
                0,
                maxLength
            );
        }

        return normalized;
    }
}
