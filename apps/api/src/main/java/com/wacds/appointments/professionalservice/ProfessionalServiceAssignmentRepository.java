package com.wacds.appointments.professionalservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de asignaciones entre Professional y Service.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Centraliza las consultas necesarias para navegar la relación:
 *
 * - Professional -> Services
 * - Service -> Professionals
 *
 * La consulta inversa Service -> Professionals se utiliza
 * especialmente para el catálogo público de reservas.
 */
public interface ProfessionalServiceAssignmentRepository
    extends JpaRepository<
        ProfessionalServiceAssignment,
        ProfessionalServiceAssignmentId
    > {

    // =========================================================
    // PROFESSIONAL -> SERVICES
    // =========================================================

    /**
     * Devuelve todos los servicios asignados a un profesional.
     *
     * Se hace JOIN FETCH del Service para evitar N+1.
     */
    @Query("""
        SELECT assignment
        FROM ProfessionalServiceAssignment assignment
        JOIN FETCH assignment.service service
        WHERE assignment.professional.id = :professionalId
        ORDER BY service.name ASC
        """)
    List<ProfessionalServiceAssignment>
        findAllWithServiceByProfessionalId(
            @Param("professionalId")
            Long professionalId
        );

    // =========================================================
    // SERVICE -> PROFESSIONALS
    // =========================================================

    /**
     * Devuelve todos los profesionales asignados a un servicio.
     *
     * Se hace JOIN FETCH del Professional para evitar N+1.
     *
     * Esta consulta es neutral respecto a:
     *
     * - active;
     * - bookingEnabled;
     * - estado del Business;
     * - publicBookingEnabled.
     *
     * Esas reglas deben aplicarse en la capa de servicio
     * correspondiente, especialmente en el catálogo público.
     */
    @Query("""
        SELECT assignment
        FROM ProfessionalServiceAssignment assignment
        JOIN FETCH assignment.professional professional
        WHERE assignment.service.id = :serviceId
        ORDER BY professional.displayName ASC, professional.id ASC
        """)
    List<ProfessionalServiceAssignment>
        findAllWithProfessionalByServiceId(
            @Param("serviceId")
            Long serviceId
        );

    // =========================================================
    // PUBLIC BOOKABLE PROFESSIONALS BY SERVICE
    // =========================================================

    /**
     * Devuelve las asignaciones de un servicio cuyos profesionales
     * pueden aceptar nuevas reservas.
     *
     * Condiciones:
     *
     * - Professional.active = true
     * - Professional.bookingEnabled = true
     *
     * IMPORTANTE:
     *
     * Esta consulta no valida por sí sola:
     *
     * - estado del Service;
     * - Service.bookingEnabled;
     * - estado del Business;
     * - Business.publicBookingEnabled;
     * - pertenencia de Service y Professional al mismo tenant.
     *
     * Esas defensas pertenecen a la capa de dominio pública.
     */
    @Query("""
        SELECT assignment
        FROM ProfessionalServiceAssignment assignment
        JOIN FETCH assignment.professional professional
        WHERE assignment.service.id = :serviceId
          AND professional.active = true
          AND professional.bookingEnabled = true
        ORDER BY professional.displayName ASC, professional.id ASC
        """)
    List<ProfessionalServiceAssignment>
        findAllBookableWithProfessionalByServiceId(
            @Param("serviceId")
            Long serviceId
        );

    // =========================================================
    // BUSINESS + SERVICE -> BOOKABLE PROFESSIONALS
    // =========================================================

    /**
     * Variante tenant-safe para obtener profesionales reservables
     * de un servicio dentro de un Business concreto.
     *
     * Esta consulta ya restringe:
     *
     * - serviceId;
     * - businessId del Professional;
     * - businessId del Service;
     * - Professional.active;
     * - Professional.bookingEnabled.
     *
     * Se mantiene la validación adicional en la capa de servicio
     * como defensa en profundidad.
     */
    @Query("""
        SELECT assignment
        FROM ProfessionalServiceAssignment assignment
        JOIN FETCH assignment.professional professional
        JOIN assignment.service service
        WHERE service.id = :serviceId
          AND service.business.id = :businessId
          AND professional.business.id = :businessId
          AND professional.active = true
          AND professional.bookingEnabled = true
        ORDER BY professional.displayName ASC, professional.id ASC
        """)
    List<ProfessionalServiceAssignment>
        findAllBookableWithProfessionalByBusinessIdAndServiceId(
            @Param("businessId")
            Long businessId,
            @Param("serviceId")
            Long serviceId
        );
}