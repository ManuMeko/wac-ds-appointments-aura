package com.wacds.appointments.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository
    extends JpaRepository<Customer, Long> {

    // =========================================================
    // BUSINESS · LIST
    // =========================================================

    /**
     * Devuelve todos los clientes pertenecientes
     * al negocio indicado.
     *
     * Incluye clientes activos e inactivos.
     */
    @Query("""
        SELECT customer
        FROM Customer customer
        WHERE customer.business.id = :businessId
        ORDER BY
            customer.firstName ASC,
            customer.lastName ASC
        """)
    List<Customer>
        findAllByBusinessIdOrderByFirstNameAscLastNameAsc(
            @Param("businessId")
            Long businessId
        );

    /**
     * Devuelve únicamente clientes activos
     * pertenecientes al negocio indicado.
     */
    @Query("""
        SELECT customer
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND customer.active = true
        ORDER BY
            customer.firstName ASC,
            customer.lastName ASC
        """)
    List<Customer>
        findAllByBusinessIdAndActiveTrueOrderByFirstNameAscLastNameAsc(
            @Param("businessId")
            Long businessId
        );

    // =========================================================
    // BUSINESS · CUSTOMER BY ID
    // =========================================================

    /**
     * Busca un cliente únicamente dentro
     * del negocio indicado.
     */
    @Query("""
        SELECT customer
        FROM Customer customer
        WHERE customer.id = :customerId
          AND customer.business.id = :businessId
        """)
    Optional<Customer>
        findByIdAndBusinessId(
            @Param("customerId")
            Long customerId,

            @Param("businessId")
            Long businessId
        );

    /**
     * Busca un cliente activo únicamente dentro
     * del negocio indicado.
     */
    @Query("""
        SELECT customer
        FROM Customer customer
        WHERE customer.id = :customerId
          AND customer.business.id = :businessId
          AND customer.active = true
        """)
    Optional<Customer>
        findByIdAndBusinessIdAndActiveTrue(
            @Param("customerId")
            Long customerId,

            @Param("businessId")
            Long businessId
        );

    // =========================================================
    // BUSINESS · EMAIL
    // =========================================================

    /**
     * Busca un cliente por email dentro
     * de un negocio concreto.
     *
     * LOWER garantiza comparación case-insensitive.
     */
    @Query("""
        SELECT customer
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND LOWER(customer.email) = LOWER(:email)
        """)
    Optional<Customer>
        findByBusinessIdAndEmailIgnoreCase(
            @Param("businessId")
            Long businessId,

            @Param("email")
            String email
        );

    /**
     * Busca un cliente activo por email
     * dentro de un negocio concreto.
     */
    @Query("""
        SELECT customer
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND customer.active = true
          AND LOWER(customer.email) = LOWER(:email)
        """)
    Optional<Customer>
        findByBusinessIdAndEmailIgnoreCaseAndActiveTrue(
            @Param("businessId")
            Long businessId,

            @Param("email")
            String email
        );

    /**
     * Comprueba si existe un cliente con ese email
     * dentro del tenant.
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(customer) > 0
            THEN true
            ELSE false
        END
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND LOWER(customer.email) = LOWER(:email)
        """)
    boolean existsByBusinessIdAndEmailIgnoreCase(
        @Param("businessId")
        Long businessId,

        @Param("email")
        String email
    );

    /**
     * Comprueba si existe OTRO cliente
     * con el mismo email dentro del tenant.
     *
     * Se utiliza durante actualizaciones.
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(customer) > 0
            THEN true
            ELSE false
        END
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND LOWER(customer.email) = LOWER(:email)
          AND customer.id <> :excludedCustomerId
        """)
    boolean existsByBusinessIdAndEmailIgnoreCaseAndIdNot(
        @Param("businessId")
        Long businessId,

        @Param("email")
        String email,

        @Param("excludedCustomerId")
        Long excludedCustomerId
    );

    // =========================================================
    // BUSINESS · PHONE
    // =========================================================

    /**
     * Busca un cliente por teléfono dentro
     * de un negocio concreto.
     */
    @Query("""
        SELECT customer
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND customer.phone = :phone
        """)
    Optional<Customer>
        findByBusinessIdAndPhone(
            @Param("businessId")
            Long businessId,

            @Param("phone")
            String phone
        );

    /**
     * Busca un cliente activo por teléfono
     * dentro del negocio indicado.
     */
    @Query("""
        SELECT customer
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND customer.active = true
          AND customer.phone = :phone
        """)
    Optional<Customer>
        findByBusinessIdAndPhoneAndActiveTrue(
            @Param("businessId")
            Long businessId,

            @Param("phone")
            String phone
        );

    /**
     * Comprueba si existe un cliente
     * con ese teléfono dentro del tenant.
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(customer) > 0
            THEN true
            ELSE false
        END
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND customer.phone = :phone
        """)
    boolean existsByBusinessIdAndPhone(
        @Param("businessId")
        Long businessId,

        @Param("phone")
        String phone
    );

    /**
     * Comprueba si existe OTRO cliente
     * con el mismo teléfono dentro del tenant.
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(customer) > 0
            THEN true
            ELSE false
        END
        FROM Customer customer
        WHERE customer.business.id = :businessId
          AND customer.phone = :phone
          AND customer.id <> :excludedCustomerId
        """)
    boolean existsByBusinessIdAndPhoneAndIdNot(
        @Param("businessId")
        Long businessId,

        @Param("phone")
        String phone,

        @Param("excludedCustomerId")
        Long excludedCustomerId
    );

    // =========================================================
    // BUSINESS · ACTIVE CHECKS
    // =========================================================

    /**
     * Comprueba que el cliente:
     *
     * - existe
     * - pertenece al negocio
     * - está activo
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(customer) > 0
            THEN true
            ELSE false
        END
        FROM Customer customer
        WHERE customer.id = :customerId
          AND customer.business.id = :businessId
          AND customer.active = true
        """)
    boolean existsByIdAndBusinessIdAndActiveTrue(
        @Param("customerId")
        Long customerId,

        @Param("businessId")
        Long businessId
    );
}