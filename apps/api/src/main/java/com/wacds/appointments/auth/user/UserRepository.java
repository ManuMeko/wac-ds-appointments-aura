package com.wacds.appointments.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
    extends JpaRepository<User, Long> {

    // =========================================================
    // FIND BY EMAIL
    // =========================================================

    /**
     * Busca un usuario por email ignorando mayúsculas/minúsculas.
     *
     * IMPORTANTE:
     *
     * Esta consulta incluye usuarios activos e inactivos.
     *
     * Es la consulta correcta para:
     *
     * - login
     * - invitaciones
     * - alta de miembros
     * - detección de usuarios ya existentes
     *
     * No debemos crear otro User simplemente porque uno existente
     * esté desactivado.
     */
    Optional<User> findByEmailIgnoreCase(
        String email
    );

    // =========================================================
    // FIND ACTIVE BY EMAIL
    // =========================================================

    /**
     * Busca un usuario activo por email.
     *
     * Puede utilizarse cuando la operación requiera explícitamente
     * una cuenta global activa.
     */
    Optional<User> findByEmailIgnoreCaseAndActiveTrue(
        String email
    );

    // =========================================================
    // EXISTS BY EMAIL
    // =========================================================

    /**
     * Comprueba si el email ya pertenece a algún usuario,
     * independientemente del estado de su cuenta.
     */
    boolean existsByEmailIgnoreCase(
        String email
    );

    /**
     * Comprueba si existe actualmente un usuario activo
     * con ese email.
     */
    boolean existsByEmailIgnoreCaseAndActiveTrue(
        String email
    );
}