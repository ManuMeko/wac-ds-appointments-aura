package com.wacds.appointments.auth.membership.dto;

import com.wacds.appointments.auth.membership.BusinessRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BusinessMembershipCreateRequest(

    // =========================================================
    // USER EMAIL
    // =========================================================

    /**
     * Email del usuario que se añadirá al business.
     *
     * Si ya existe un User con este email:
     *
     * - se reutilizará ese usuario
     * - no se cambiará su contraseña
     * - no se sobrescribirá su perfil
     *
     * Si no existe:
     *
     * - se creará un nuevo User
     * - firstName será obligatorio
     * - lastName será obligatorio
     * - temporaryPassword será obligatorio
     */
    @NotBlank(
        message = "El email es obligatorio."
    )
    @Email(
        message = "El email no tiene un formato válido."
    )
    @Size(
        max = 255,
        message = "El email no puede superar los 255 caracteres."
    )
    String email,

    // =========================================================
    // FIRST NAME
    // =========================================================

    /**
     * Obligatorio únicamente cuando el email todavía
     * no pertenece a ningún User.
     */
    @Size(
        max = 100,
        message = "El nombre no puede superar los 100 caracteres."
    )
    String firstName,

    // =========================================================
    // LAST NAME
    // =========================================================

    /**
     * Obligatorio únicamente cuando el email todavía
     * no pertenece a ningún User.
     */
    @Size(
        max = 150,
        message = "Los apellidos no pueden superar los 150 caracteres."
    )
    String lastName,

    // =========================================================
    // TEMPORARY PASSWORD
    // =========================================================

    /**
     * Contraseña inicial para un usuario completamente nuevo.
     *
     * Nunca se almacena en texto plano.
     *
     * BusinessMembershipManagementService deberá codificarla
     * mediante PasswordEncoder antes de persistir el User.
     *
     * Si el usuario ya existe, este campo será ignorado.
     *
     * En una evolución posterior sustituiremos este mecanismo
     * por un flujo formal de invitación con token de un solo uso.
     */
    @Size(
        min = 12,
        max = 72,
        message = "La contraseña temporal debe contener entre 12 y 72 caracteres."
    )
    String temporaryPassword,

    // =========================================================
    // BUSINESS ROLE
    // =========================================================

    /**
     * Rol que tendrá el miembro dentro del business.
     *
     * Las reglas de autorización NO se aplican aquí.
     *
     * BusinessMembershipManagementService comprobará:
     *
     * OWNER:
     * - puede crear ADMIN
     * - puede crear STAFF
     *
     * ADMIN:
     * - puede crear únicamente STAFF
     *
     * STAFF:
     * - no puede crear miembros
     *
     * OWNER nunca podrá asignarse mediante este endpoint.
     */
    @NotNull(
        message = "El rol es obligatorio."
    )
    BusinessRole role

) {
}