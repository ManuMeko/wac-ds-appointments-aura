package com.wacds.appointments.appointment.dto;

import com.wacds.appointments.appointment.Appointment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * DTO utilizado para crear una cita desde la API privada
 * del negocio.
 *
 * IMPORTANTE
 * ==========
 *
 * Este DTO pertenece exclusivamente al contexto Business.
 *
 * No debe reutilizarse directamente en endpoints públicos
 * de reserva, ya que permite trabajar con identificadores
 * internos del negocio como:
 *
 * - customerId;
 * - professionalId;
 * - serviceId;
 * - estado inicial de Appointment.
 *
 * La reserva pública utiliza su propio contrato
 * (PublicBookingRequest).
 *
 * De esta forma se mantienen claramente separados:
 *
 * - el flujo administrativo Business;
 * - el flujo público de reservas.
 *
 * La lógica de negocio y las validaciones contextuales
 * continúan perteneciendo a AppointmentService.
 */
public record AppointmentRequest(

    // =========================================================
    // CUSTOMER
    // =========================================================

    /**
     * Cliente existente dentro del mismo negocio
     * al que pertenece la cita.
     */
    @NotNull(
        message = "customerId es obligatorio."
    )
    @Positive(
        message = "customerId debe ser mayor que 0."
    )
    Long customerId,

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    /**
     * Profesional que realizará el servicio.
     *
     * Debe pertenecer al mismo negocio de la cita.
     */
    @NotNull(
        message = "professionalId es obligatorio."
    )
    @Positive(
        message = "professionalId debe ser mayor que 0."
    )
    Long professionalId,

    // =========================================================
    // SERVICE
    // =========================================================

    /**
     * Servicio reservado.
     *
     * Debe pertenecer al mismo negocio de la cita.
     */
    @NotNull(
        message = "serviceId es obligatorio."
    )
    @Positive(
        message = "serviceId debe ser mayor que 0."
    )
    Long serviceId,

    // =========================================================
    // START
    // =========================================================

    /**
     * Fecha y hora de inicio de la cita.
     *
     * Debe incluir offset.
     *
     * Ejemplo:
     *
     * 2026-10-26T17:40:00+01:00
     *
     * AppointmentService continúa siendo responsable de
     * validar:
     *
     * - que la fecha sea válida para crear una cita;
     * - disponibilidad del profesional;
     * - horario laboral;
     * - excepciones de agenda;
     * - compatibilidad profesional/servicio;
     * - duración;
     * - solapamientos;
     * - pertenencia al tenant;
     * - reglas de negocio adicionales.
     */
    @NotNull(
        message = "startAt es obligatorio."
    )
    OffsetDateTime startAt,

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Estado inicial solicitado desde Business.
     *
     * Puede ser null.
     *
     * Si no se especifica, AppointmentMapper utiliza
     * PENDING como estado inicial.
     *
     * AppointmentService sigue siendo responsable de validar
     * que el estado solicitado sea aceptable para una
     * creación administrativa.
     */
    Appointment.AppointmentStatus status,

    // =========================================================
    // NOTES
    // =========================================================

    /**
     * Notas internas opcionales asociadas a la cita.
     */
    @Size(
        max = 5000,
        message = "Las notas no pueden superar los 5000 caracteres."
    )
    String notes

) {

    // =========================================================
    // COMPACT CONSTRUCTOR
    // =========================================================

    /**
     * Realiza únicamente normalización ligera del DTO.
     *
     * No debe introducir lógica de negocio.
     */
    public AppointmentRequest {

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