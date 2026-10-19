package com.wacds.appointments.availabilityslot;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.availabilityslot.dto.DailyAvailabilityResponse;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Objects;

/**
 * API HTTP del motor de disponibilidad de
 * WAC D.S. Appointments.
 *
 * Expone dos fachadas sobre el mismo motor:
 *
 * PRIVADA
 * -------
 *
 * GET /api/v1/availability-slots
 *
 * Requiere:
 *
 * - JWT;
 * - Business activo en sesión;
 * - rol operativo OWNER / ADMIN / STAFF.
 *
 * La disponibilidad privada continúa funcionando aunque
 * el negocio haya desactivado las reservas públicas.
 *
 * PÚBLICA
 * -------
 *
 * GET /api/v1/public/availability-slots
 *
 * No requiere:
 *
 * - cuenta;
 * - JWT;
 * - membresía Business.
 *
 * Además de las reglas normales de disponibilidad exige:
 *
 * - Business activo;
 * - reservas públicas habilitadas.
 *
 * MOTOR COMPARTIDO
 * ----------------
 *
 * Ambos endpoints utilizan:
 *
 * AvailabilitySlotService.getDailyAvailability(...)
 *
 * De esta forma todas las reglas de:
 *
 * - horarios;
 * - excepciones;
 * - servicios;
 * - profesionales;
 * - asignaciones;
 * - citas existentes;
 * - buffers;
 * - intervalos;
 * - horizonte;
 * - aviso mínimo;
 * - timezone;
 *
 * permanecen centralizadas en un único motor.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class AvailabilitySlotController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AvailabilitySlotService
        availabilitySlotService;

    private final BusinessMembershipService
        businessMembershipService;

    private final BusinessRepository
        businessRepository;

    // =========================================================
    // PRIVATE · BUSINESS PANEL
    // =========================================================

    /**
     * Obtiene la disponibilidad diaria utilizada por
     * el panel privado del negocio.
     *
     * Endpoint:
     *
     * GET /api/v1/availability-slots
     *
     * Ejemplo:
     *
     * /api/v1/availability-slots
     *     ?professionalId=1
     *     &serviceId=1
     *     &date=2026-10-15
     *
     * IMPORTANTE:
     *
     * Este endpoint NO depende de publicBookingEnabled.
     */
    @GetMapping(
        "/availability-slots"
    )
    public ResponseEntity<DailyAvailabilityResponse>
        getPrivateDailyAvailability(

            @RequestParam
            @NotNull(
                message = "professionalId es obligatorio."
            )
            @Positive(
                message = "professionalId debe ser mayor que 0."
            )
            Long professionalId,

            @RequestParam
            @NotNull(
                message = "serviceId es obligatorio."
            )
            @Positive(
                message = "serviceId debe ser mayor que 0."
            )
            Long serviceId,

            @RequestParam
            @NotNull(
                message = "date es obligatorio."
            )
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date

        ) {

        // -----------------------------------------------------
        // AUTHENTICATION
        // -----------------------------------------------------

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long currentBusinessId =
            authenticatedUser.getBusinessId();

        // -----------------------------------------------------
        // AUTHORIZATION
        // -----------------------------------------------------

        businessMembershipService
            .requireOperationalAccess(
                authenticatedUser,
                currentBusinessId
            );

        // -----------------------------------------------------
        // AVAILABILITY
        // -----------------------------------------------------

        DailyAvailabilityResponse response =
            calculateAvailability(
                professionalId,
                serviceId,
                date
            );

        // -----------------------------------------------------
        // TENANT DEFENCE
        // -----------------------------------------------------

        validateResponseBelongsToCurrentBusiness(
            response,
            currentBusinessId
        );

        // -----------------------------------------------------
        // RESPONSE
        // -----------------------------------------------------

        return buildNoStoreResponse(
            response
        );
    }

    // =========================================================
    // PUBLIC · BOOKING WEBSITE
    // =========================================================

    /**
     * Obtiene la disponibilidad diaria utilizada por
     * el flujo público de reservas.
     *
     * Endpoint:
     *
     * GET /api/v1/public/availability-slots
     *
     * Ejemplo:
     *
     * /api/v1/public/availability-slots
     *     ?professionalId=1
     *     &serviceId=1
     *     &date=2026-10-19
     *
     * No requiere autenticación.
     *
     * El motor central valida:
     *
     * - Professional existente;
     * - Professional activo;
     * - Professional.bookingEnabled;
     * - Service existente;
     * - Service activo;
     * - Service.bookingEnabled;
     * - mismo tenant;
     * - asignación Professional <-> Service;
     * - disponibilidad semanal;
     * - excepciones;
     * - citas existentes;
     * - buffers;
     * - slotIntervalMinutes;
     * - minimumBookingNoticeMinutes;
     * - bookingHorizonDays;
     * - timezone.
     *
     * Esta fachada pública valida adicionalmente:
     *
     * - Business activo;
     * - publicBookingEnabled.
     */
    @GetMapping(
        "/public/availability-slots"
    )
    public ResponseEntity<DailyAvailabilityResponse>
        getPublicDailyAvailability(

            @RequestParam
            @NotNull(
                message = "professionalId es obligatorio."
            )
            @Positive(
                message = "professionalId debe ser mayor que 0."
            )
            Long professionalId,

            @RequestParam
            @NotNull(
                message = "serviceId es obligatorio."
            )
            @Positive(
                message = "serviceId debe ser mayor que 0."
            )
            Long serviceId,

            @RequestParam
            @NotNull(
                message = "date es obligatorio."
            )
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date

        ) {

        // -----------------------------------------------------
        // AVAILABILITY ENGINE
        // -----------------------------------------------------

        DailyAvailabilityResponse response =
            calculateAvailability(
                professionalId,
                serviceId,
                date
            );

        // -----------------------------------------------------
        // PUBLIC BUSINESS POLICY
        // -----------------------------------------------------

        validatePublicBookingAvailable(
            response
        );

        // -----------------------------------------------------
        // RESPONSE
        // -----------------------------------------------------

        return buildNoStoreResponse(
            response
        );
    }

    // =========================================================
    // AVAILABILITY ENGINE
    // =========================================================

    /**
     * Centraliza el acceso al motor de disponibilidad.
     *
     * No realiza autorización ni aplica reglas específicas
     * del endpoint público/privado.
     */
    private DailyAvailabilityResponse calculateAvailability(

        Long professionalId,
        Long serviceId,
        LocalDate date

    ) {

        if (
            professionalId == null
                || professionalId <= 0
        ) {

            throw new IllegalArgumentException(
                "professionalId no es válido."
            );
        }

        if (
            serviceId == null
                || serviceId <= 0
        ) {

            throw new IllegalArgumentException(
                "serviceId no es válido."
            );
        }

        if (
            date == null
        ) {

            throw new IllegalArgumentException(
                "date es obligatorio."
            );
        }

        DailyAvailabilityResponse response =
            availabilitySlotService
                .getDailyAvailability(
                    professionalId,
                    serviceId,
                    date
                );

        if (
            response == null
        ) {

            throw new IllegalStateException(
                "El motor de disponibilidad ha devuelto "
                    + "una respuesta nula."
            );
        }

        return response;
    }

    // =========================================================
    // PUBLIC BUSINESS VALIDATION
    // =========================================================

    /**
     * Comprueba que el Business propietario de la
     * disponibilidad pueda aceptar reservas públicas.
     *
     * No aceptamos businessId desde el cliente.
     *
     * El Business se obtiene indirectamente después de que
     * el motor haya validado Professional, Service y tenant.
     */
    private void validatePublicBookingAvailable(

        DailyAvailabilityResponse response

    ) {

        if (
            response == null
        ) {

            throw new IllegalStateException(
                "La respuesta de disponibilidad no es válida."
            );
        }

        Long businessId =
            response.businessId();

        if (
            businessId == null
                || businessId <= 0
        ) {

            throw new IllegalStateException(
                "La respuesta de disponibilidad "
                    + "no contiene un businessId válido."
            );
        }

        Business business =
            businessRepository
                .findById(
                    businessId
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No se ha encontrado el negocio "
                                + "asociado a la disponibilidad."
                        )
                );

        if (
            !business.canAcceptPublicBookings()
        ) {

            throw new ConflictException(
                "Las reservas públicas no están disponibles "
                    + "actualmente para este negocio."
            );
        }
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    /**
     * La disponibilidad es información volátil.
     *
     * Nunca debe cachearse porque puede cambiar después de:
     *
     * - crear una cita;
     * - cancelar una cita;
     * - reprogramar;
     * - cambiar horarios;
     * - crear/eliminar excepciones;
     * - cambiar configuración;
     * - activar/desactivar reservas públicas.
     */
    private ResponseEntity<DailyAvailabilityResponse>
        buildNoStoreResponse(

            DailyAvailabilityResponse response

        ) {

        if (
            response == null
        ) {

            throw new IllegalStateException(
                "El motor de disponibilidad ha devuelto "
                    + "una respuesta nula."
            );
        }

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
    // SECURITY · AUTHENTICATED USER
    // =========================================================

    /**
     * Recupera y valida el principal establecido por
     * JwtAuthenticationFilter.
     */
    private AuthenticatedUser requireAuthenticatedUser() {

        Authentication authentication =
            SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (
            authentication == null
                || !authentication.isAuthenticated()
        ) {

            throw new BadCredentialsException(
                "No existe una sesión autenticada válida."
            );
        }

        Object principal =
            authentication.getPrincipal();

        if (
            !(principal
                instanceof AuthenticatedUser authenticatedUser)
        ) {

            throw new BadCredentialsException(
                "El principal autenticado no es válido."
            );
        }

        if (
            authenticatedUser.getUserId() == null
                || authenticatedUser.getUserId() <= 0
        ) {

            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un userId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() == null
                || authenticatedUser.getBusinessId() <= 0
        ) {

            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un businessId válido."
            );
        }

        if (
            authenticatedUser.getRole() == null
        ) {

            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un rol válido."
            );
        }

        return authenticatedUser;
    }

    // =========================================================
    // TENANT VALIDATION
    // =========================================================

    /**
     * Defensa adicional del endpoint privado.
     *
     * Professional y Service pueden ser válidos entre sí, pero
     * además deben pertenecer exactamente al Business activo
     * de la sesión.
     */
    private void validateResponseBelongsToCurrentBusiness(

        DailyAvailabilityResponse response,
        Long currentBusinessId

    ) {

        if (
            response == null
        ) {

            throw new IllegalStateException(
                "La respuesta de disponibilidad no es válida."
            );
        }

        if (
            currentBusinessId == null
                || currentBusinessId <= 0
        ) {

            throw new BadCredentialsException(
                "La sesión no contiene un businessId válido."
            );
        }

        if (
            response.businessId() == null
                || response.businessId() <= 0
        ) {

            throw new IllegalStateException(
                "La respuesta de disponibilidad "
                    + "no contiene businessId válido."
            );
        }

        if (
            !Objects.equals(
                response.businessId(),
                currentBusinessId
            )
        ) {

            /*
             * No revelamos información sobre el Business
             * propietario real del recurso solicitado.
             */
            throw new AccessDeniedException(
                "No tienes acceso a la disponibilidad solicitada."
            );
        }
    }
}