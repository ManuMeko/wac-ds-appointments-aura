package com.wacds.appointments.business.settings;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessSettingsRepository
    extends JpaRepository<BusinessSettings, Long> {

    // =========================================================
    // FIND BY BUSINESS
    // =========================================================

    /**
     * Recupera la configuración operativa de un business.
     *
     * Se carga también Business para evitar problemas
     * de LazyInitialization cuando posteriormente necesitemos:
     *
     * - businessId
     * - validaciones tenant
     * - mapping a DTO
     */
    @EntityGraph(
        attributePaths = {
            "business"
        }
    )
    Optional<BusinessSettings> findByBusinessId(
        Long businessId
    );

    // =========================================================
    // EXISTS
    // =========================================================

    /**
     * Comprueba si un business ya dispone de configuración.
     *
     * La relación es 1:1 y PostgreSQL también la protege
     * mediante UNIQUE(business_id).
     */
    boolean existsByBusinessId(
        Long businessId
    );

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Eliminación explícita por business.
     *
     * Normalmente no será necesario utilizarlo porque la FK
     * tiene ON DELETE CASCADE, pero queda disponible para
     * operaciones internas y tests.
     */
    void deleteByBusinessId(
        Long businessId
    );
}