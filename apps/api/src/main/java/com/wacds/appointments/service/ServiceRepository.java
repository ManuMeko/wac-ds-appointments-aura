package com.wacds.appointments.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Service.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Centraliza las consultas de servicios utilizadas tanto
 * por el panel privado Business como por futuras fachadas
 * públicas de reserva.
 *
 * =========================================================
 * TENANT
 * =========================================================
 *
 * Todas las consultas específicas del negocio deben incluir
 * businessId.
 *
 * De este modo evitamos que un Service perteneciente a otro
 * Business pueda entrar accidentalmente en una operación
 * tenant-aware.
 */
public interface ServiceRepository
    extends JpaRepository<Service, Long> {

    // =========================================================
    // BUSINESS · ALL
    // =========================================================

    /**
     * Devuelve todos los servicios de un Business.
     */
    List<Service> findAllByBusinessIdOrderByNameAsc(
        Long businessId
    );

    // =========================================================
    // BUSINESS · BY ID
    // =========================================================

    /**
     * Busca un Service concreto dentro de un Business.
     */
    Optional<Service> findByIdAndBusinessId(
        Long id,
        Long businessId
    );

    // =========================================================
    // BUSINESS · BY SLUG
    // =========================================================

    /**
     * Busca un Service por slug dentro de un Business.
     */
    Optional<Service> findByBusinessIdAndSlug(
        Long businessId,
        String slug
    );

    /**
     * Comprueba si existe un slug concreto dentro del Business.
     */
    boolean existsByBusinessIdAndSlug(
        Long businessId,
        String slug
    );

    // =========================================================
    // BUSINESS · ACTIVE
    // =========================================================

    /**
     * Devuelve únicamente servicios activos.
     *
     * Puede utilizarse en contextos administrativos u
     * operativos donde bookingEnabled no sea requisito.
     */
    List<Service> findAllByBusinessIdAndActiveTrueOrderByNameAsc(
        Long businessId
    );

    // =========================================================
    // BUSINESS · PUBLICLY BOOKABLE
    // =========================================================

    /**
     * Devuelve únicamente servicios que pueden participar
     * en un flujo de nueva reserva.
     *
     * Condiciones:
     *
     * - pertenecen al Business indicado;
     * - active = true;
     * - bookingEnabled = true.
     *
     * IMPORTANTE:
     *
     * Esta consulta NO valida por sí sola:
     *
     * - Business activo;
     * - publicBookingEnabled;
     * - existencia de profesionales compatibles;
     * - disponibilidad real.
     *
     * Esas reglas deben aplicarse en la capa de servicio
     * pública correspondiente.
     */
    List<Service>
        findAllByBusinessIdAndActiveTrueAndBookingEnabledTrueOrderByNameAsc(
            Long businessId
        );
}