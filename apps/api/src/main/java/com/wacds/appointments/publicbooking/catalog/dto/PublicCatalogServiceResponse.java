package com.wacds.appointments.publicbooking.catalog.dto;

import java.math.BigDecimal;

/**
 * Servicio visible dentro del catálogo público de reservas.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Este DTO expone exclusivamente la información necesaria
 * para que un cliente pueda seleccionar un Service desde
 * la web pública.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * No expone:
 *
 * - entidad JPA Service;
 * - Business completo;
 * - flags administrativos internos;
 * - timestamps;
 * - información sensible del negocio;
 * - relaciones internas.
 *
 * =========================================================
 * REGLAS
 * =========================================================
 *
 * Este DTO solo debe construirse para servicios que:
 *
 * - pertenezcan al Business solicitado;
 * - estén activos;
 * - tengan bookingEnabled = true;
 * - pertenezcan a un Business operativo;
 * - pertenezcan a un Business con reservas públicas activas.
 *
 * La capa PublicCatalogService será responsable de aplicar
 * esas reglas antes de crear esta respuesta.
 */
public record PublicCatalogServiceResponse(

    /**
     * Identificador del Service.
     *
     * Se utilizará posteriormente para:
     *
     * - consultar profesionales compatibles;
     * - consultar disponibilidad;
     * - crear la reserva.
     */
    Long id,

    /**
     * Slug público del servicio.
     *
     * Ejemplo:
     *
     * tratamiento-facial
     */
    String slug,

    /**
     * Nombre visible del servicio.
     */
    String name,

    /**
     * Descripción pública del servicio.
     */
    String description,

    /**
     * Duración del servicio en minutos.
     */
    Integer durationMinutes,

    /**
     * Precio público del servicio.
     */
    BigDecimal price,

    /**
     * Moneda ISO 4217 del Business.
     *
     * Ejemplo:
     *
     * EUR
     */
    String currency

) {
}