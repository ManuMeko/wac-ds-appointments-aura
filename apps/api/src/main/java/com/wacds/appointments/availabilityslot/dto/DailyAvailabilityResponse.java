package com.wacds.appointments.availabilityslot.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta diaria del motor de disponibilidad.
 *
 * Este DTO es compartido por:
 *
 * - el panel privado Business;
 * - el flujo público de reservas.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Representa la disponibilidad real de un Professional
 * para un Service concreto y una fecha determinada.
 *
 * Incluye tanto información contextual como los slots
 * calculados por AvailabilitySlotService.
 *
 * =========================================================
 * TENANT
 * =========================================================
 *
 * businessId identifica el Business propietario de:
 *
 * - Professional;
 * - Service;
 * - disponibilidad resultante.
 *
 * En el flujo público este valor permite que el frontend
 * conozca el businessId validado por backend sin tener que
 * confiar en un identificador hardcodeado o suministrado
 * arbitrariamente por el cliente.
 *
 * =========================================================
 * DISPONIBILIDAD
 * =========================================================
 *
 * available indica si existe al menos un horario disponible.
 *
 * totalSlots contiene el número total de slots incluidos
 * en la respuesta.
 *
 * slots contiene el detalle exacto de cada horario.
 *
 * =========================================================
 * TIMEZONE
 * =========================================================
 *
 * timezone contiene la zona horaria IANA utilizada por
 * el Business.
 *
 * Ejemplo:
 *
 * Europe/Madrid
 */
public record DailyAvailabilityResponse(

    // =========================================================
    // BUSINESS
    // =========================================================

    /**
     * Business propietario de la disponibilidad.
     */
    Long businessId,

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    /**
     * Profesional sobre el que se ha calculado
     * la disponibilidad.
     */
    Long professionalId,

    /**
     * Nombre visible del profesional.
     */
    String professionalName,

    // =========================================================
    // SERVICE
    // =========================================================

    /**
     * Servicio solicitado.
     */
    Long serviceId,

    /**
     * Nombre visible del servicio.
     */
    String serviceName,

    /**
     * Duración del servicio en minutos.
     */
    Integer serviceDurationMinutes,

    // =========================================================
    // DATE
    // =========================================================

    /**
     * Fecha sobre la que se ha calculado
     * la disponibilidad.
     */
    LocalDate date,

    // =========================================================
    // TIMEZONE
    // =========================================================

    /**
     * Zona horaria IANA del negocio.
     *
     * Ejemplo:
     *
     * Europe/Madrid
     */
    String timezone,

    // =========================================================
    // AVAILABILITY
    // =========================================================

    /**
     * Indica si existe al menos un slot disponible.
     */
    boolean available,

    /**
     * Número total de slots incluidos en la respuesta.
     */
    int totalSlots,

    /**
     * Slots disponibles calculados por el motor.
     */
    List<AvailabilitySlotResponse> slots

) {
}