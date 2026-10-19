package com.wacds.appointments.publicbooking.management;

import com.wacds.appointments.publicbooking.dto.PublicBookingCancelResponse;
import com.wacds.appointments.publicbooking.dto.PublicBookingManagementResponse;
import com.wacds.appointments.publicbooking.dto.PublicBookingRescheduleRequest;
import com.wacds.appointments.publicbooking.dto.PublicBookingRescheduleResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pública de gestión de reservas.
 *
 * Permite a un cliente consultar y gestionar su propia cita
 * mediante una credencial segura de gestión.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * No utiliza appointmentId como mecanismo de autorización.
 *
 * La autorización pública se basa exclusivamente en:
 *
 * X-Booking-Management-Token
 *
 * El token:
 *
 * - es aleatorio y no predecible;
 * - no se almacena en texto plano;
 * - se transforma a SHA-256 en backend;
 * - se valida contra appointment_management_tokens;
 * - puede expirar;
 * - puede revocarse.
 *
 * =========================================================
 * TRANSPORTE DEL TOKEN
 * =========================================================
 *
 * Deliberadamente utilizamos un header y no una URL del tipo:
 *
 * /manage/{token}
 *
 * para reducir la exposición accidental del secreto en:
 *
 * - logs del servidor;
 * - históricos del navegador;
 * - analytics;
 * - reverse proxies;
 * - herramientas de monitorización.
 *
 * El frontend debe conservar temporalmente el token recibido
 * durante el flujo público y enviarlo exclusivamente mediante
 * el header definido en este controlador.
 *
 * =========================================================
 * CACHE
 * =========================================================
 *
 * Todas las respuestas de este controlador utilizan:
 *
 * Cache-Control: no-store
 *
 * porque contienen información asociada a una reserva gestionada
 * mediante una credencial secreta y su estado puede cambiar
 * inmediatamente después de:
 *
 * - cancelación;
 * - reprogramación;
 * - cambios administrativos;
 * - revocación del management token.
 *
 * =========================================================
 * RESPONSABILIDAD
 * =========================================================
 *
 * Este controller únicamente:
 *
 * - recibe la petición HTTP;
 * - extrae el management token;
 * - valida el body mediante Jakarta Validation;
 * - delega en PublicBookingManagementService;
 * - construye la respuesta HTTP.
 *
 * NO contiene lógica de:
 *
 * - autenticación del token;
 * - Appointment;
 * - disponibilidad;
 * - reglas de cancelación;
 * - reglas de reprogramación;
 * - concurrencia;
 * - reminders;
 * - revocación de tokens.
 */
@RestController
@RequestMapping(
    value = "/api/v1/public/bookings/manage",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class PublicBookingManagementController {

    // =========================================================
    // CONSTANTS
    // =========================================================

    /**
     * Header utilizado para transportar la credencial
     * secreta de gestión pública.
     *
     * IMPORTANTE:
     *
     * Este valor no debe registrarse deliberadamente en logs.
     */
    public static final String MANAGEMENT_TOKEN_HEADER =
        "X-Booking-Management-Token";

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final PublicBookingManagementService
        publicBookingManagementService;

    // =========================================================
    // GET BOOKING
    // =========================================================

    /**
     * Recupera la vista pública de gestión de una reserva.
     *
     * Endpoint:
     *
     * GET /api/v1/public/bookings/manage
     *
     * Header obligatorio:
     *
     * X-Booking-Management-Token: <managementToken>
     *
     * Devuelve información pública de:
     *
     * - cita;
     * - estado;
     * - negocio;
     * - servicio;
     * - profesional;
     * - horario;
     * - políticas de cancelación;
     * - políticas de reprogramación;
     * - deadlines;
     * - acciones actualmente permitidas.
     */
    @GetMapping
    public ResponseEntity<PublicBookingManagementResponse>
        getBooking(

            @RequestHeader(
                name = MANAGEMENT_TOKEN_HEADER
            )
            String managementToken
        ) {

        PublicBookingManagementResponse response =
            publicBookingManagementService
                .getBooking(
                    managementToken
                );

        return ResponseEntity
            .ok()
            .cacheControl(
                CacheControl.noStore()
            )
            .body(
                response
            );
    }

    // =========================================================
    // CANCEL BOOKING
    // =========================================================

    /**
     * Cancela una reserva pública utilizando exclusivamente
     * el management token.
     *
     * Endpoint:
     *
     * POST /api/v1/public/bookings/manage/cancel
     *
     * Header obligatorio:
     *
     * X-Booking-Management-Token: <managementToken>
     *
     * No requiere:
     *
     * - JWT Business;
     * - appointmentId;
     * - businessId;
     * - customerId.
     *
     * PublicBookingManagementService valida:
     *
     * - token existente;
     * - token no expirado;
     * - token no revocado;
     * - Appointment asociada;
     * - BusinessSettings;
     * - onlineCancellationEnabled;
     * - estado cancelable;
     * - minimumCancellationNoticeMinutes;
     * - concurrencia sobre la agenda.
     *
     * Después de persistir correctamente CANCELLED:
     *
     * - los reminders activos de la Appointment se cancelan;
     * - los management tokens activos quedan revocados.
     *
     * Los reminders SENT históricos permanecen conservados.
     */
    @PostMapping(
        value = "/cancel"
    )
    public ResponseEntity<PublicBookingCancelResponse>
        cancelBooking(

            @RequestHeader(
                name = MANAGEMENT_TOKEN_HEADER
            )
            String managementToken
        ) {

        PublicBookingCancelResponse response =
            publicBookingManagementService
                .cancelBooking(
                    managementToken
                );

        return ResponseEntity
            .ok()
            .cacheControl(
                CacheControl.noStore()
            )
            .body(
                response
            );
    }

    // =========================================================
    // RESCHEDULE BOOKING
    // =========================================================

    /**
     * Reprograma una reserva pública mediante
     * management token.
     *
     * Endpoint:
     *
     * POST /api/v1/public/bookings/manage/reschedule
     *
     * Header obligatorio:
     *
     * X-Booking-Management-Token: <managementToken>
     *
     * Body:
     *
     * {
     *   "startAt": "2026-10-19T17:00:00+02:00"
     * }
     *
     * El cliente únicamente puede solicitar un nuevo startAt.
     *
     * NO puede modificar directamente:
     *
     * - appointmentId;
     * - businessId;
     * - customerId;
     * - professionalId;
     * - serviceId;
     * - duration;
     * - endAt;
     * - status.
     *
     * PublicBookingManagementService vuelve a validar:
     *
     * - management token;
     * - estado de la reserva;
     * - BusinessSettings;
     * - onlineReschedulingEnabled;
     * - minimumRescheduleNoticeMinutes;
     * - disponibilidad;
     * - agenda;
     * - excepciones;
     * - buffers;
     * - solapamientos;
     * - concurrencia.
     *
     * Después de persistir correctamente el nuevo horario:
     *
     * AppointmentReminderService sincroniza los reminders.
     *
     * Esto permite:
     *
     * - conservar reminders SENT históricos;
     * - cancelar reminders activos de ocurrencias anteriores;
     * - generar reminders correspondientes al nuevo startAt.
     *
     * La Appointment conserva:
     *
     * - su mismo id;
     * - su Customer;
     * - su Service;
     * - su Professional;
     * - su management token.
     */
    @PostMapping(
        value = "/reschedule",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublicBookingRescheduleResponse>
        rescheduleBooking(

            @RequestHeader(
                name = MANAGEMENT_TOKEN_HEADER
            )
            String managementToken,

            @Valid
            @RequestBody
            PublicBookingRescheduleRequest request
        ) {

        PublicBookingRescheduleResponse response =
            publicBookingManagementService
                .rescheduleBooking(
                    managementToken,
                    request
                );

        return ResponseEntity
            .ok()
            .cacheControl(
                CacheControl.noStore()
            )
            .body(
                response
            );
    }
}