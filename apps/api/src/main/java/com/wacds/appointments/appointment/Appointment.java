
package com.wacds.appointments.appointment;

import com.wacds.appointments.business.Business;
import com.wacds.appointments.customer.Customer;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.service.Service;

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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Entidad principal de reservas y citas.
 *
 * RESPONSABILIDADES:
 *
 * - Asociar una cita a un negocio.
 * - Asociar un cliente.
 * - Asociar un profesional.
 * - Asociar un servicio.
 * - Almacenar el intervalo temporal.
 * - Mantener el estado de la cita.
 * - Conservar notas y auditoria.
 *
 * MULTITENANCY:
 *
 * Business, Customer, Professional y Service
 * deben corresponder al mismo negocio.
 *
 * CONCURRENCIA:
 *
 * Esta entidad valida la integridad de una cita,
 * pero no adquiere bloqueos de agenda.
 *
 * Las operaciones de creacion y reprogramacion
 * deben protegerse mediante el bloqueo
 * transaccional del profesional.
 *
 * El bloqueo se gestiona desde
 * AppointmentRepository y los servicios.
 *
 * IMPORTANTE:
 *
 * No se introducen nuevas columnas, indices,
 * relaciones ni migraciones.
 */
@Entity
@Table(
    name = "appointments",
    indexes = {
        @Index(
            name = "idx_appointments_business_id",
            columnList = "business_id"
        ),
        @Index(
            name = "idx_appointments_customer_id",
            columnList = "customer_id"
        ),
        @Index(
            name = "idx_appointments_professional_id",
            columnList = "professional_id"
        ),
        @Index(
            name = "idx_appointments_service_id",
            columnList = "service_id"
        ),
        @Index(
            name = "idx_appointments_professional_start",
            columnList = "professional_id, start_at"
        ),
        @Index(
            name = "idx_appointments_business_start",
            columnList = "business_id, start_at"
        ),
        @Index(
            name = "idx_appointments_status",
            columnList = "status"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Estados del ciclo de vida de una cita.
     */
    public enum AppointmentStatus {

        /**
         * Pendiente de confirmacion.
         */
        PENDING,

        /**
         * Confirmada.
         */
        CONFIRMED,

        /**
         * Servicio realizado.
         */
        COMPLETED,

        /**
         * Cancelada.
         */
        CANCELLED,

        /**
         * Cliente no presentado.
         */
        NO_SHOW
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
    // BUSINESS
    // =========================================================

    /**
     * Negocio propietario de la cita.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "business_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_appointments_business"
        )
    )
    private Business business;

    // =========================================================
    // CUSTOMER
    // =========================================================

    /**
     * Cliente que realiza la reserva.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "customer_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_appointments_customer"
        )
    )
    private Customer customer;

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    /**
     * Profesional que realiza el servicio.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "professional_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_appointments_professional"
        )
    )
    private Professional professional;

    // =========================================================
    // SERVICE
    // =========================================================

    /**
     * Servicio reservado.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "service_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_appointments_service"
        )
    )
    private Service service;

    // =========================================================
    // TIME
    // =========================================================

    /**
     * Inicio real de la cita.
     *
     * OffsetDateTime conserva un instante
     * temporal inequívoco.
     */
    @Column(
        name = "start_at",
        nullable = false
    )
    private OffsetDateTime startAt;

    /**
     * Final real de la cita.
     *
     * Debe ser posterior a startAt.
     */
    @Column(
        name = "end_at",
        nullable = false
    )
    private OffsetDateTime endAt;

    // =========================================================
    // APPOINTMENT STATUS
    // =========================================================

    /**
     * Estado inicial predeterminado.
     *
     * PublicBookingService puede establecer
     * CONFIRMED si el negocio utiliza
     * confirmacion automatica.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    private AppointmentStatus status =
        AppointmentStatus.PENDING;

    // =========================================================
    // NOTES
    // =========================================================

    /**
     * Observaciones opcionales.
     */
    @Column(
        columnDefinition = "TEXT"
    )
    private String notes;

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
     * Validacion antes de INSERT.
     */
    @PrePersist
    void prePersist() {

        normalize();

        validate();

        OffsetDateTime now =
            OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    /**
     * Validacion antes de UPDATE.
     */
    @PreUpdate
    void preUpdate() {

        normalize();

        validate();

        updatedAt = OffsetDateTime.now();
    }

    // =========================================================
    // GLOBAL VALIDATION
    // =========================================================

    /**
     * Comprueba la integridad de la entidad.
     *
     * No comprueba disponibilidad ni
     * solapamientos con otras citas.
     *
     * Esas operaciones corresponden
     * a los servicios transaccionales.
     */
    private void validate() {

        validateRelationships();

        validateTenantConsistency();

        validateTemporalConsistency();

        validateStatus();
    }

    // =========================================================
    // RELATIONSHIP VALIDATION
    // =========================================================

    private void validateRelationships() {

        if (
            business == null
                || business.getId() == null
        ) {
            throw new IllegalStateException(
                "La cita debe pertenecer "
                    + "a un negocio persistido."
            );
        }

        if (
            customer == null
                || customer.getId() == null
        ) {
            throw new IllegalStateException(
                "La cita debe tener "
                    + "un cliente persistido."
            );
        }

        if (
            professional == null
                || professional.getId() == null
        ) {
            throw new IllegalStateException(
                "La cita debe tener "
                    + "un profesional persistido."
            );
        }

        if (
            service == null
                || service.getId() == null
        ) {
            throw new IllegalStateException(
                "La cita debe tener "
                    + "un servicio persistido."
            );
        }
    }

    // =========================================================
    // TENANT CONSISTENCY
    // =========================================================

    /**
     * Evita crear citas relacionadas con
     * entidades de negocios distintos.
     *
     * Se ejecuta antes de INSERT y UPDATE.
     */
    private void validateTenantConsistency() {

        Long businessId =
            business.getId();

        Long customerBusinessId =
            customer.getBusinessId();

        if (
            !Objects.equals(
                businessId,
                customerBusinessId
            )
        ) {
            throw new IllegalStateException(
                "El cliente no pertenece "
                    + "al negocio de la cita."
            );
        }

        if (
            professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "El profesional no tiene "
                    + "un negocio válido asociado."
            );
        }

        Long professionalBusinessId =
            professional.getBusiness().getId();

        if (
            !Objects.equals(
                businessId,
                professionalBusinessId
            )
        ) {
            throw new IllegalStateException(
                "El profesional no pertenece "
                    + "al negocio de la cita."
            );
        }

        if (
            service.getBusiness() == null
                || service.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "El servicio no tiene "
                    + "un negocio válido asociado."
            );
        }

        Long serviceBusinessId =
            service.getBusiness().getId();

        if (
            !Objects.equals(
                businessId,
                serviceBusinessId
            )
        ) {
            throw new IllegalStateException(
                "El servicio no pertenece "
                    + "al negocio de la cita."
            );
        }
    }

    // =========================================================
    // TEMPORAL CONSISTENCY
    // =========================================================

    /**
     * Una cita debe contener siempre
     * un intervalo temporal válido.
     *
     * Intervalo:
     *
     * [startAt, endAt)
     */
    private void validateTemporalConsistency() {

        if (startAt == null) {
            throw new IllegalStateException(
                "La fecha de inicio "
                    + "de la cita es obligatoria."
            );
        }

        if (endAt == null) {
            throw new IllegalStateException(
                "La fecha de finalización "
                    + "de la cita es obligatoria."
            );
        }

        if (!endAt.isAfter(startAt)) {
            throw new IllegalStateException(
                "La fecha de finalización de la cita "
                    + "debe ser posterior a la fecha de inicio."
            );
        }
    }

    // =========================================================
    // STATUS VALIDATION
    // =========================================================

    private void validateStatus() {

        if (status == null) {
            throw new IllegalStateException(
                "El estado de la cita es obligatorio."
            );
        }
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private void normalize() {

        notes = normalizeNullableText(notes);
    }

    private String normalizeNullableText(
        String value
    ) {

        if (
            value == null
                || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    // =========================================================
    // TENANT HELPERS
    // =========================================================

    /**
     * Identificador del negocio propietario.
     */
    public Long getBusinessId() {

        if (
            business == null
                || business.getId() == null
        ) {
            return null;
        }

        return business.getId();
    }

    /**
     * Comprueba pertenencia al negocio.
     */
    public boolean belongsToBusiness(
        Long businessId
    ) {

        return businessId != null
            && Objects.equals(
                getBusinessId(),
                businessId
            );
    }

    // =========================================================
    // RELATION IDS
    // =========================================================

    public Long getCustomerId() {

        if (
            customer == null
                || customer.getId() == null
        ) {
            return null;
        }

        return customer.getId();
    }

    public Long getProfessionalId() {

        if (
            professional == null
                || professional.getId() == null
        ) {
            return null;
        }

        return professional.getId();
    }

    public Long getServiceId() {

        if (
            service == null
                || service.getId() == null
        ) {
            return null;
        }

        return service.getId();
    }

    // =========================================================
    // STATUS HELPERS
    // =========================================================

    public boolean isPending() {

        return status ==
            AppointmentStatus.PENDING;
    }

    public boolean isConfirmed() {

        return status ==
            AppointmentStatus.CONFIRMED;
    }

    public boolean isCompleted() {

        return status ==
            AppointmentStatus.COMPLETED;
    }

    public boolean isCancelled() {

        return status ==
            AppointmentStatus.CANCELLED;
    }

    public boolean isNoShow() {

        return status ==
            AppointmentStatus.NO_SHOW;
    }

    // =========================================================
    // AVAILABILITY STATUS
    // =========================================================

    /**
     * Determina si la cita ocupa agenda.
     *
     * PENDING   -> ocupa
     * CONFIRMED -> ocupa
     * COMPLETED -> ocupa
     * NO_SHOW   -> ocupa
     * CANCELLED -> no ocupa
     *
     * Un estado nulo es inconsistente
     * y se rechaza.
     */
    public boolean blocksAvailability() {

        if (status == null) {
            throw new IllegalStateException(
                "No se puede calcular la ocupación "
                    + "de una cita sin estado."
            );
        }

        return status !=
            AppointmentStatus.CANCELLED;
    }

    // =========================================================
    // TERMINAL STATUS
    // =========================================================

    /**
     * Estados terminales.
     */
    public boolean isTerminalStatus() {

        return status ==
                AppointmentStatus.COMPLETED
            || status ==
                AppointmentStatus.CANCELLED
            || status ==
                AppointmentStatus.NO_SHOW;
    }

    // =========================================================
    // RESCHEDULING
    // =========================================================

    /**
     * Solo las citas pendientes o confirmadas
     * pueden reprogramarse.
     */
    public boolean isReschedulable() {

        return status ==
                AppointmentStatus.PENDING
            || status ==
                AppointmentStatus.CONFIRMED;
    }

    // =========================================================
    // TIME HELPERS
    // =========================================================

    /**
     * Duracion de la cita.
     *
     * Devuelve Duration.ZERO cuando
     * no existe intervalo completo.
     */
    public Duration getDuration() {

        if (
            startAt == null
                || endAt == null
        ) {
            return Duration.ZERO;
        }

        return Duration.between(
            startAt,
            endAt
        );
    }

    /**
     * Duracion expresada en minutos.
     */
    public long getDurationMinutes() {

        return getDuration().toMinutes();
    }

    // =========================================================
    // INTERVAL OVERLAP
    // =========================================================

    /**
     * Comprueba si la cita se solapa
     * con otro intervalo.
     *
     * Se utilizan intervalos semiabiertos:
     *
     * [startAt, endAt)
     *
     * Formula:
     *
     * startA < endB && endA > startB
     *
     * Dos citas que solo se tocan
     * en un extremo no se solapan.
     *
     * Los buffers operativos se comprueban
     * en los servicios de disponibilidad
     * y creacion de reservas.
     */
    public boolean overlaps(
        OffsetDateTime otherStart,
        OffsetDateTime otherEnd
    ) {

        if (
            startAt == null
                || endAt == null
                || otherStart == null
                || otherEnd == null
        ) {
            return false;
        }

        if (
            !endAt.isAfter(startAt)
                || !otherEnd.isAfter(otherStart)
        ) {
            return false;
        }

        return startAt.isBefore(otherEnd)
            && endAt.isAfter(otherStart);
    }
}
