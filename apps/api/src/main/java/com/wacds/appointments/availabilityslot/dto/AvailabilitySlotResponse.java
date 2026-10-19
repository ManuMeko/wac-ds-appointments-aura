package com.wacds.appointments.availabilityslot.dto;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Representa un slot concreto de disponibilidad.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Este DTO contiene tanto la representación local de la hora
 * como los timestamps completos con offset.
 *
 * Esto permite que el frontend:
 *
 * - muestre horas simples al usuario;
 * - conserve el instante exacto calculado por backend;
 * - envíe directamente startAt al crear la reserva;
 * - evite reconstrucciones manuales de timezone u offset.
 *
 * =========================================================
 * FRONTEND
 * =========================================================
 *
 * Para presentación:
 *
 * - startTime;
 * - endTime.
 *
 * Para creación de la reserva:
 *
 * - startAt.
 *
 * endAt se devuelve como información complementaria, pero
 * PublicBookingService será quien calcule y valide el final
 * definitivo de la cita.
 */
public record AvailabilitySlotResponse(

    /**
     * Hora local de inicio del slot.
     *
     * Ejemplo:
     *
     * 11:20:00
     */
    LocalTime startTime,

    /**
     * Hora local de finalización del slot.
     *
     * Ejemplo:
     *
     * 12:05:00
     */
    LocalTime endTime,

    /**
     * Inicio exacto del slot con offset.
     *
     * Este es el valor que debe utilizar el frontend como
     * startAt al crear una reserva pública.
     *
     * Ejemplo:
     *
     * 2026-10-19T11:20:00+02:00
     */
    OffsetDateTime startAt,

    /**
     * Final exacto del slot con offset.
     *
     * Se expone principalmente para presentación y contexto.
     * El backend continúa siendo la fuente de verdad sobre la
     * duración efectiva de la cita.
     */
    OffsetDateTime endAt

) {
}