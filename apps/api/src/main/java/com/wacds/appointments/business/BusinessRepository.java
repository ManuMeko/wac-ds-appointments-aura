package com.wacds.appointments.business;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de Business.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Centraliza las consultas de negocio utilizadas por:
 *
 * - administración Business;
 * - autenticación;
 * - configuración;
 * - flujos públicos basados en slug.
 *
 * =========================================================
 * SLUG
 * =========================================================
 *
 * El slug actúa como identificador público estable del negocio.
 *
 * Ejemplo:
 *
 * aura-studio
 *
 * Esto permite resolver el Business desde la web pública
 * sin exponer ni hardcodear directamente businessId.
 */
public interface BusinessRepository
    extends JpaRepository<Business, Long> {

    // =========================================================
    // FIND BY SLUG
    // =========================================================

    /**
     * Busca un Business por slug ignorando mayúsculas/minúsculas.
     *
     * Aunque el slug debe mantenerse normalizado en lowercase,
     * esta variante añade una defensa adicional.
     */
    Optional<Business> findBySlugIgnoreCase(
        String slug
    );

    // =========================================================
    // FIND BY EMAIL
    // =========================================================

    /**
     * Busca un Business por email ignorando
     * mayúsculas/minúsculas.
     */
    Optional<Business> findByEmailIgnoreCase(
        String email
    );

    // =========================================================
    // EXISTS BY SLUG
    // =========================================================

    /**
     * Comprueba si existe un Business con el slug indicado.
     */
    boolean existsBySlugIgnoreCase(
        String slug
    );

    /**
     * Permite comprobar unicidad durante una actualización
     * excluyendo el Business actual.
     */
    boolean existsBySlugIgnoreCaseAndIdNot(
        String slug,
        Long id
    );

    // =========================================================
    // EXISTS BY EMAIL
    // =========================================================

    /**
     * Comprueba si existe un Business con el email indicado.
     */
    boolean existsByEmailIgnoreCase(
        String email
    );

    /**
     * Permite comprobar colisiones de email durante una
     * actualización excluyendo el Business actual.
     */
    boolean existsByEmailIgnoreCaseAndIdNot(
        String email,
        Long id
    );
}