
package com.wacds.appointments.professionalservice;

import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.service.Service;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Asignacion de un Service a un Professional.
 *
 * TABLA:
 *
 * professional_services
 *
 * CLAVE PRIMARIA COMPUESTA:
 *
 * - professional_id
 * - service_id
 *
 * RESPONSABILIDADES:
 *
 * - Asociar servicios y profesionales.
 * - Mantener la identidad compuesta.
 * - Validar la pertenencia al mismo negocio.
 * - Evitar referencias incompletas.
 * - Conservar la fecha de creacion.
 *
 * MULTITENANCY:
 *
 * Un profesional solo puede asociarse
 * a servicios de su propio negocio.
 *
 * CONCURRENCIA:
 *
 * Esta entidad no gestiona bloqueos.
 *
 * El control de reservas concurrentes
 * pertenece a PublicBookingService
 * y AppointmentRepository.
 *
 * COMPATIBILIDAD:
 *
 * Se mantienen:
 *
 * - Tabla.
 * - Indices.
 * - Clave primaria compuesta.
 * - Relaciones @ManyToOne.
 * - @MapsId.
 * - Builder Lombok.
 * - Getters y setters existentes.
 *
 * No necesita migraciones Flyway.
 */
@Entity
@Table(
    name = "professional_services",
    indexes = {
        @Index(
            name = "idx_professional_services_professional_id",
            columnList = "professional_id"
        ),
        @Index(
            name = "idx_professional_services_service_id",
            columnList = "service_id"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalServiceAssignment {

    // =========================================================
    // COMPOSITE ID
    // =========================================================

    /**
     * Clave primaria compuesta.
     *
     * Los valores deben corresponder
     * a las entidades relacionadas.
     */
    @EmbeddedId
    private ProfessionalServiceAssignmentId id;

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    /**
     * Profesional que ofrece el servicio.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @MapsId("professionalId")
    @JoinColumn(
        name = "professional_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_professional_services_professional"
        )
    )
    private Professional professional;

    // =========================================================
    // SERVICE
    // =========================================================

    /**
     * Servicio asignado al profesional.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @MapsId("serviceId")
    @JoinColumn(
        name = "service_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_professional_services_service"
        )
    )
    private Service service;

    // =========================================================
    // AUDIT
    // =========================================================

    /**
     * Fecha de creacion de la asignacion.
     */
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    // =========================================================
    // DOMAIN FACTORY
    // =========================================================

    /**
     * Crea una asignacion validada.
     *
     * Requiere que Professional y Service
     * ya tengan identificadores persistidos.
     *
     * Es compatible con:
     *
     * ProfessionalServiceAssignment.create(
     *     professional,
     *     service
     * );
     *
     * Utilizado por servicios de negocio
     * y pruebas de integracion.
     */
    public static ProfessionalServiceAssignment create(
        Professional professional,
        Service service
    ) {

        validateRelationships(
            professional,
            service
        );

        validateSameBusiness(
            professional,
            service
        );

        ProfessionalServiceAssignmentId assignmentId =
            new ProfessionalServiceAssignmentId(
                professional.getId(),
                service.getId()
            );

        return ProfessionalServiceAssignment.builder()
            .id(assignmentId)
            .professional(professional)
            .service(service)
            .build();
    }

    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    /**
     * Valida antes de insertar.
     */
    @PrePersist
    void prePersist() {

        validateAndSynchronize();

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    /**
     * Valida antes de actualizar.
     *
     * No modifica createdAt.
     */
    @PreUpdate
    void preUpdate() {

        validateAndSynchronize();
    }

    // =========================================================
    // ENTITY VALIDATION
    // =========================================================

    /**
     * Valida relaciones, negocio propietario
     * e identidad compuesta.
     */
    private void validateAndSynchronize() {

        validateRelationships(
            professional,
            service
        );

        validateSameBusiness(
            professional,
            service
        );

        Long professionalId =
            professional.getId();

        Long serviceId =
            service.getId();

        if (id == null) {

            id = new ProfessionalServiceAssignmentId(
                professionalId,
                serviceId
            );

            return;
        }

        if (
            !Objects.equals(
                id.getProfessionalId(),
                professionalId
            )
                || !Objects.equals(
                    id.getServiceId(),
                    serviceId
                )
        ) {
            throw new IllegalStateException(
                "La clave compuesta de la asignación "
                    + "no coincide con el profesional "
                    + "y el servicio asociados."
            );
        }
    }

    // =========================================================
    // RELATIONSHIP VALIDATION
    // =========================================================

    /**
     * Requiere entidades existentes
     * con IDs válidos.
     */
    private static void validateRelationships(
        Professional professional,
        Service service
    ) {

        if (professional == null) {
            throw new IllegalArgumentException(
                "El profesional es obligatorio."
            );
        }

        if (
            professional.getId() == null
                || professional.getId() <= 0
        ) {
            throw new IllegalStateException(
                "El profesional debe estar persistido "
                    + "antes de crear la asignación."
            );
        }

        if (service == null) {
            throw new IllegalArgumentException(
                "El servicio es obligatorio."
            );
        }

        if (
            service.getId() == null
                || service.getId() <= 0
        ) {
            throw new IllegalStateException(
                "El servicio debe estar persistido "
                    + "antes de crear la asignación."
            );
        }
    }

    // =========================================================
    // TENANT VALIDATION
    // =========================================================

    /**
     * Impide relacionar entidades
     * de negocios diferentes.
     */
    private static void validateSameBusiness(
        Professional professional,
        Service service
    ) {

        if (
            professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "El profesional no tiene "
                    + "un negocio válido asociado."
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

        Long professionalBusinessId =
            professional.getBusiness().getId();

        Long serviceBusinessId =
            service.getBusiness().getId();

        if (
            !Objects.equals(
                professionalBusinessId,
                serviceBusinessId
            )
        ) {
            throw new IllegalStateException(
                "No se puede asignar un servicio "
                    + "de otro negocio al profesional."
            );
        }
    }

    // =========================================================
    // DOMAIN HELPERS
    // =========================================================

    /**
     * Comprueba la pertenencia
     * a un profesional.
     */
    public boolean belongsToProfessional(
        Long professionalId
    ) {

        return professionalId != null
            && professional != null
            && Objects.equals(
                professional.getId(),
                professionalId
            );
    }

    /**
     * Comprueba la pertenencia
     * a un servicio.
     */
    public boolean belongsToService(
        Long serviceId
    ) {

        return serviceId != null
            && service != null
            && Objects.equals(
                service.getId(),
                serviceId
            );
    }

    /**
     * Devuelve el negocio propietario
     * sin exponer su entidad.
     */
    public Long getBusinessId() {

        if (
            professional == null
                || professional.getBusiness() == null
        ) {
            return null;
        }

        return professional.getBusiness().getId();
    }

    /**
     * Comprueba si esta asignacion
     * pertenece al negocio indicado.
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
}
