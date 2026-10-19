package com.wacds.appointments.publicbooking.catalog.dto;

/**
 * Profesional visible dentro del catálogo público
 * de reservas.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Este DTO expone únicamente la información necesaria para
 * que un cliente pueda seleccionar un Professional desde
 * el flujo público de reservas.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * No expone:
 *
 * - entidad JPA Professional;
 * - email;
 * - teléfono;
 * - Business completo;
 * - flags administrativos internos;
 * - timestamps;
 * - información operativa privada.
 *
 * =========================================================
 * REGLAS
 * =========================================================
 *
 * Este DTO solo debe construirse para profesionales que:
 *
 * - pertenezcan al Business correcto;
 * - estén activos;
 * - tengan bookingEnabled = true;
 * - estén asignados al Service seleccionado;
 * - pertenezcan a un Business operativo;
 * - pertenezcan a un Business con reservas públicas activas.
 *
 * La capa PublicCatalogService será responsable de aplicar
 * estas reglas antes de construir la respuesta.
 */
public record PublicCatalogProfessionalResponse(

    /**
     * Identificador del Professional.
     *
     * Se utilizará posteriormente para:
     *
     * - consultar disponibilidad;
     * - seleccionar fecha y hora;
     * - crear la reserva.
     */
    Long id,

    /**
     * Nombre público que se mostrará al cliente.
     *
     * Ejemplo:
     *
     * Laura Martín
     *
     * o:
     *
     * Lucía · Especialista facial
     */
    String displayName,

    /**
     * Biografía o descripción pública del profesional.
     *
     * Puede ser null cuando el Business no haya configurado
     * una descripción pública.
     */
    String bio

) {
}