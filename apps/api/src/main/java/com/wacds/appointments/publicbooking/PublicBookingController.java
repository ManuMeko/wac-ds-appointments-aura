package com.wacds.appointments.publicbooking;

import com.wacds.appointments.publicbooking.dto.PublicBookingRequest;
import com.wacds.appointments.publicbooking.dto.PublicBookingResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pública de reservas de WAC D.S. Appointments.
 *
 * Este controlador representa la fachada HTTP pública
 * utilizada por clientes finales desde la web pública
 * del negocio.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * La creación pública de reservas:
 *
 * - es anónima;
 * - no requiere JWT Business;
 * - no requiere sesión Business;
 * - no requiere membresía OWNER / ADMIN / STAFF.
 *
 * La seguridad multi-tenant y todas las reglas de dominio
 * deben ejecutarse dentro de PublicBookingService.
 *
 * =========================================================
 * RESPONSABILIDAD DEL CONTROLLER
 * =========================================================
 *
 * Este controller únicamente debe:
 *
 * - recibir la petición HTTP;
 * - ejecutar Jakarta Bean Validation;
 * - delegar en PublicBookingService;
 * - devolver el código HTTP correspondiente.
 *
 * =========================================================
 * RESPONSABILIDADES QUE NO PERTENECEN AQUÍ
 * =========================================================
 *
 * Este controller NO debe implementar directamente:
 *
 * - resolución de Business;
 * - BusinessSettings;
 * - aislamiento multi-tenant;
 * - Professional;
 * - Service;
 * - asignación Professional <-> Service;
 * - disponibilidad semanal;
 * - excepciones de agenda;
 * - buffers;
 * - solapamientos;
 * - concurrencia;
 * - Customer;
 * - Appointment;
 * - auto confirmación;
 * - recordatorios;
 * - management tokens;
 * - cancelación;
 * - reprogramación.
 *
 * Toda esa lógica pertenece a las capas de dominio.
 *
 * =========================================================
 * GESTIÓN POSTERIOR DE LA RESERVA
 * =========================================================
 *
 * Consulta, cancelación y reprogramación pública de una
 * reserva NO deben autorizarse únicamente mediante
 * appointmentId.
 *
 * Dichas operaciones pertenecen a:
 *
 * publicbooking.management
 *
 * y utilizan management tokens específicos asociados
 * a cada Appointment.
 */
@RestController
@RequestMapping(
    value = "/api/v1/public/bookings",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class PublicBookingController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final PublicBookingService
        publicBookingService;

    // =========================================================
    // CREATE PUBLIC BOOKING
    // =========================================================

    /**
     * Crea una nueva reserva desde la web pública.
     *
     * Endpoint:
     *
     * POST /api/v1/public/bookings
     *
     * Seguridad:
     *
     * - anónimo;
     * - sin JWT Business;
     * - sin membresía interna.
     *
     * La forma exacta del payload la define:
     *
     * PublicBookingRequest
     *
     * PublicBookingService debe validar, entre otras reglas:
     *
     * - Business existente;
     * - Business activo;
     * - reservas públicas habilitadas;
     * - Professional existente;
     * - Professional activo;
     * - Professional.bookingEnabled;
     * - Service existente;
     * - Service activo;
     * - Service.bookingEnabled;
     * - aislamiento multi-tenant;
     * - asignación Professional <-> Service;
     * - minimumBookingNoticeMinutes;
     * - bookingHorizonDays;
     * - disponibilidad semanal;
     * - excepciones;
     * - buffers;
     * - solapamientos;
     * - concurrencia;
     * - creación o reutilización de Customer;
     * - autoConfirmAppointments;
     * - creación de management token;
     * - sincronización de reminders.
     *
     * Respuesta:
     *
     * HTTP 201 CREATED
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublicBookingResponse>
        createBooking(

            @Valid
            @RequestBody
            PublicBookingRequest request
        ) {

        PublicBookingResponse response =
            publicBookingService.createBooking(
                request
            );

        return ResponseEntity
            .status(
                HttpStatus.CREATED
            )
            .body(
                response
            );
    }
}