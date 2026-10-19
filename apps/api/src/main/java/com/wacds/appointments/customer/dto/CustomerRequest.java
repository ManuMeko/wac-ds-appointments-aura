package com.wacds.appointments.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado por la API privada Business
 * para crear o actualizar clientes.
 *
 * Este contrato contiene únicamente datos editables
 * del cliente.
 *
 * La lógica relacionada con:
 *
 * - tenant;
 * - unicidad;
 * - activación/desactivación;
 * - reglas de negocio;
 * - permisos;
 *
 * pertenece a CustomerService.
 */
public record CustomerRequest(

    // =========================================================
    // FIRST NAME
    // =========================================================

    @NotBlank(
        message = "El nombre del cliente es obligatorio."
    )
    @Size(
        max = 100,
        message = "El nombre no puede superar los 100 caracteres."
    )
    String firstName,

    // =========================================================
    // LAST NAME
    // =========================================================

    @Size(
        max = 150,
        message = "Los apellidos no pueden superar los 150 caracteres."
    )
    String lastName,

    // =========================================================
    // EMAIL
    // =========================================================

    @Email(
        message = "El email del cliente no tiene un formato válido."
    )
    @Size(
        max = 180,
        message = "El email no puede superar los 180 caracteres."
    )
    String email,

    // =========================================================
    // PHONE
    // =========================================================

    @Size(
        max = 40,
        message = "El teléfono no puede superar los 40 caracteres."
    )
    String phone,

    // =========================================================
    // NOTES
    // =========================================================

    @Size(
        max = 5000,
        message = "Las notas no pueden superar los 5000 caracteres."
    )
    String notes,

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Puede ser null.
     *
     * En creación, CustomerService puede aplicar el valor
     * por defecto correspondiente.
     *
     * En edición permite activar o desactivar al cliente.
     */
    Boolean active

) {

    // =========================================================
    // COMPACT CONSTRUCTOR
    // =========================================================

    /**
     * Normalización ligera de datos de entrada.
     *
     * No contiene lógica de negocio.
     */
    public CustomerRequest {

        if (
            firstName != null
        ) {
            firstName =
                firstName.trim();
        }

        if (
            lastName != null
        ) {
            lastName =
                lastName.trim();

            if (
                lastName.isEmpty()
            ) {
                lastName =
                    null;
            }
        }

        if (
            email != null
        ) {
            email =
                email.trim()
                    .toLowerCase();

            if (
                email.isEmpty()
            ) {
                email =
                    null;
            }
        }

        if (
            phone != null
        ) {
            phone =
                phone.trim();

            if (
                phone.isEmpty()
            ) {
                phone =
                    null;
            }
        }

        if (
            notes != null
        ) {
            notes =
                notes.trim();

            if (
                notes.isEmpty()
            ) {
                notes =
                    null;
            }
        }
    }
}