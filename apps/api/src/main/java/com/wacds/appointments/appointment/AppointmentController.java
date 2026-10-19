package com.wacds.appointments.appointment;

import com.wacds.appointments.appointment.dto.AppointmentRequest;
import com.wacds.appointments.appointment.dto.AppointmentRescheduleRequest;
import com.wacds.appointments.appointment.dto.AppointmentResponse;
import com.wacds.appointments.appointment.dto.AppointmentStatusUpdateRequest;
import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class AppointmentController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AppointmentService appointmentService;

    private final BusinessMembershipService businessMembershipService;

    // =========================================================
    // CREATE · CURRENT BUSINESS
    // =========================================================

    /**
     * Crea una cita dentro del negocio autenticado.
     *
     * Endpoint principal para el panel Business.
     *
     * El frontend no necesita enviar businessId.
     *
     * POST /api/v1/appointments
     */
    @PostMapping("/api/v1/appointments")
    public ResponseEntity<AppointmentResponse> createForCurrentBusiness(

        @Valid
        @RequestBody
        AppointmentRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        AppointmentResponse response =
            appointmentService.create(
                businessId,
                request
            );

        validateAppointmentBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    // =========================================================
    // CREATE · EXPLICIT BUSINESS
    // =========================================================

    /**
     * Ruta legacy mantenida por compatibilidad.
     */
    @PostMapping("/api/v1/businesses/{businessId}/appointments")
    public ResponseEntity<AppointmentResponse> create(

        @PathVariable
        Long businessId,

        @Valid
        @RequestBody
        AppointmentRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        AppointmentResponse response =
            appointmentService.create(
                businessId,
                request
            );

        validateAppointmentBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    // =========================================================
    // STATUS
    // =========================================================

    @PatchMapping("/api/v1/appointments/{appointmentId}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(

        @PathVariable
        Long appointmentId,

        @Valid
        @RequestBody
        AppointmentStatusUpdateRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        AppointmentResponse existing =
            appointmentService
                .findByBusinessAndId(
                    businessId,
                    appointmentId
                );

        validateAppointmentBusiness(
            authenticatedUser,
            existing
        );

        AppointmentResponse response =
            appointmentService.updateStatus(
                appointmentId,
                request
            );

        validateAppointmentBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // RESCHEDULE
    // =========================================================

    @PatchMapping("/api/v1/appointments/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentResponse> reschedule(

        @PathVariable
        Long appointmentId,

        @Valid
        @RequestBody
        AppointmentRescheduleRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        AppointmentResponse existing =
            appointmentService
                .findByBusinessAndId(
                    businessId,
                    appointmentId
                );

        validateAppointmentBusiness(
            authenticatedUser,
            existing
        );

        AppointmentResponse response =
            appointmentService.reschedule(
                appointmentId,
                request
            );

        validateAppointmentBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // CURRENT BUSINESS · LIST
    // =========================================================

    /**
     * Listado principal de citas del negocio autenticado.
     *
     * Sin parámetros:
     *
     * GET /api/v1/appointments
     *
     * Devuelve todas las citas del negocio.
     *
     * Con fecha:
     *
     * GET /api/v1/appointments?date=2026-10-14
     *
     * Devuelve únicamente las citas correspondientes a ese
     * día según la zona horaria del negocio.
     *
     * Con fecha y estado:
     *
     * GET /api/v1/appointments
     *     ?date=2026-10-14
     *     &status=CANCELLED
     *
     * El parámetro status solamente se aplica cuando también
     * existe date. De esta forma se conserva el contrato
     * histórico del endpoint cuando se solicita el listado
     * completo.
     */
    @GetMapping("/api/v1/appointments")
    public ResponseEntity<List<AppointmentResponse>>
        findAllForCurrentBusiness(

            @RequestParam(required = false)
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date,

            @RequestParam(required = false)
            Appointment.AppointmentStatus status,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        List<AppointmentResponse> appointments;

        if (date != null) {

            appointments =
                appointmentService
                    .findBusinessAgendaForDay(
                        businessId,
                        date,
                        status
                    );

        } else {

            appointments =
                appointmentService
                    .findAllByBusiness(
                        businessId
                    );
        }

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // BUSINESS · LIST · LEGACY
    // =========================================================

    /**
     * Ruta legacy equivalente al listado principal.
     *
     * Sin date devuelve todas las citas.
     *
     * Si se proporciona date, devuelve únicamente la agenda
     * del día indicado.
     */
    @GetMapping("/api/v1/businesses/{businessId}/appointments")
    public ResponseEntity<List<AppointmentResponse>>
        findAllByBusiness(

            @PathVariable
            Long businessId,

            @RequestParam(required = false)
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date,

            @RequestParam(required = false)
            Appointment.AppointmentStatus status,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        List<AppointmentResponse> appointments;

        if (date != null) {

            appointments =
                appointmentService
                    .findBusinessAgendaForDay(
                        businessId,
                        date,
                        status
                    );

        } else {

            appointments =
                appointmentService
                    .findAllByBusiness(
                        businessId
                    );
        }

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // FIND BY ID · CURRENT BUSINESS
    // =========================================================

    @GetMapping("/api/v1/appointments/{appointmentId}")
    public ResponseEntity<AppointmentResponse> findById(

        @PathVariable
        Long appointmentId,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        AppointmentResponse response =
            appointmentService
                .findByBusinessAndId(
                    businessId,
                    appointmentId
                );

        validateAppointmentBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY BUSINESS AND ID · LEGACY
    // =========================================================

    @GetMapping(
        "/api/v1/businesses/{businessId}/appointments/{appointmentId}"
    )
    public ResponseEntity<AppointmentResponse>
        findByBusinessAndId(

            @PathVariable
            Long businessId,

            @PathVariable
            Long appointmentId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        AppointmentResponse response =
            appointmentService
                .findByBusinessAndId(
                    businessId,
                    appointmentId
                );

        validateAppointmentBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // PROFESSIONAL · APPOINTMENTS
    // =========================================================

    @GetMapping(
        "/api/v1/professionals/{professionalId}/appointments"
    )
    public ResponseEntity<List<AppointmentResponse>>
        findAllByProfessional(

            @PathVariable
            Long professionalId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        requireCurrentBusinessAccess(
            authenticatedUser
        );

        List<AppointmentResponse> appointments =
            appointmentService
                .findAllByProfessional(
                    professionalId
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // CUSTOMER · APPOINTMENTS
    // =========================================================

    @GetMapping(
        "/api/v1/customers/{customerId}/appointments"
    )
    public ResponseEntity<List<AppointmentResponse>>
        findAllByCustomer(

            @PathVariable
            Long customerId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        requireCurrentBusinessAccess(
            authenticatedUser
        );

        List<AppointmentResponse> appointments =
            appointmentService
                .findAllByCustomer(
                    customerId
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // CURRENT BUSINESS AGENDA · DAY
    // =========================================================

    /**
     * Endpoint explícito de agenda diaria.
     *
     * GET /api/v1/appointments/day?date=2026-10-26
     *
     * Opcional:
     *
     * &status=CONFIRMED
     */
    @GetMapping("/api/v1/appointments/day")
    public ResponseEntity<List<AppointmentResponse>>
        findCurrentBusinessAgendaForDay(

            @RequestParam
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date,

            @RequestParam(required = false)
            Appointment.AppointmentStatus status,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        List<AppointmentResponse> appointments =
            appointmentService
                .findBusinessAgendaForDay(
                    businessId,
                    date,
                    status
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // BUSINESS AGENDA · DAY · LEGACY
    // =========================================================

    @GetMapping(
        "/api/v1/businesses/{businessId}/appointments/day"
    )
    public ResponseEntity<List<AppointmentResponse>>
        findBusinessAgendaForDay(

            @PathVariable
            Long businessId,

            @RequestParam
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date,

            @RequestParam(required = false)
            Appointment.AppointmentStatus status,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        List<AppointmentResponse> appointments =
            appointmentService
                .findBusinessAgendaForDay(
                    businessId,
                    date,
                    status
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // CURRENT BUSINESS AGENDA · RANGE
    // =========================================================

    /**
     * GET /api/v1/appointments/range
     *     ?from=2026-10-26
     *     &to=2026-11-01
     *
     * Opcional:
     *
     * &status=CONFIRMED
     */
    @GetMapping("/api/v1/appointments/range")
    public ResponseEntity<List<AppointmentResponse>>
        findCurrentBusinessAgendaInRange(

            @RequestParam
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate from,

            @RequestParam
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate to,

            @RequestParam(required = false)
            Appointment.AppointmentStatus status,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        List<AppointmentResponse> appointments =
            appointmentService
                .findBusinessAgendaInRange(
                    businessId,
                    from,
                    to,
                    status
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // BUSINESS AGENDA · RANGE · LEGACY
    // =========================================================

    @GetMapping(
        "/api/v1/businesses/{businessId}/appointments/range"
    )
    public ResponseEntity<List<AppointmentResponse>>
        findBusinessAgendaInRange(

            @PathVariable
            Long businessId,

            @RequestParam
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate from,

            @RequestParam
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate to,

            @RequestParam(required = false)
            Appointment.AppointmentStatus status,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        List<AppointmentResponse> appointments =
            appointmentService
                .findBusinessAgendaInRange(
                    businessId,
                    from,
                    to,
                    status
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // CURRENT BUSINESS · UPCOMING
    // =========================================================

    /**
     * GET /api/v1/appointments/upcoming
     */
    @GetMapping("/api/v1/appointments/upcoming")
    public ResponseEntity<List<AppointmentResponse>>
        findUpcomingCurrentBusinessAppointments(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        List<AppointmentResponse> appointments =
            appointmentService
                .findUpcomingBusinessAppointments(
                    businessId
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // BUSINESS · UPCOMING · LEGACY
    // =========================================================

    @GetMapping(
        "/api/v1/businesses/{businessId}/appointments/upcoming"
    )
    public ResponseEntity<List<AppointmentResponse>>
        findUpcomingBusinessAppointments(

            @PathVariable
            Long businessId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        List<AppointmentResponse> appointments =
            appointmentService
                .findUpcomingBusinessAppointments(
                    businessId
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // PROFESSIONAL AGENDA · RANGE
    // =========================================================

    @GetMapping(
        "/api/v1/professionals/{professionalId}/appointments/range"
    )
    public ResponseEntity<List<AppointmentResponse>>
        findProfessionalAgendaInRange(

            @PathVariable
            Long professionalId,

            @RequestParam
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate from,

            @RequestParam
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate to,

            @RequestParam(required = false)
            Appointment.AppointmentStatus status,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser

        ) {

        requireCurrentBusinessAccess(
            authenticatedUser
        );

        List<AppointmentResponse> appointments =
            appointmentService
                .findProfessionalAgendaInRange(
                    professionalId,
                    from,
                    to,
                    status
                );

        validateAppointmentListBusiness(
            authenticatedUser,
            appointments
        );

        return ResponseEntity.ok(
            appointments
        );
    }

    // =========================================================
    // SECURITY HELPERS
    // =========================================================

    /**
     * Devuelve el businessId del tenant autenticado.
     *
     * Antes valida:
     *
     * - que exista usuario autenticado;
     * - que exista businessId;
     * - que la membresía siga siendo válida.
     */
    private Long requireCurrentBusinessId(
        AuthenticatedUser authenticatedUser
    ) {

        requireCurrentBusinessAccess(
            authenticatedUser
        );

        return authenticatedUser
            .getBusinessId();
    }

    /**
     * Comprueba que la sesión sigue teniendo acceso
     * al negocio contenido en su JWT.
     */
    private void requireCurrentBusinessAccess(
        AuthenticatedUser authenticatedUser
    ) {

        if (
            authenticatedUser == null
                || authenticatedUser.getBusinessId() == null
        ) {

            throw new AccessDeniedException(
                "No existe un contexto de negocio autenticado válido."
            );
        }

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                authenticatedUser.getBusinessId()
            );
    }

    // =========================================================
    // TENANT DEFENCE · SINGLE APPOINTMENT
    // =========================================================

    /**
     * Comprueba defensivamente que una cita devuelta
     * pertenece exactamente al tenant autenticado.
     */
    private void validateAppointmentBusiness(

        AuthenticatedUser authenticatedUser,

        AppointmentResponse appointment

    ) {

        if (
            appointment == null
        ) {

            throw new AccessDeniedException(
                "No se puede validar el acceso a la cita."
            );
        }

        requireCurrentBusinessAccess(
            authenticatedUser
        );

        if (
            appointment.businessId() == null
                || !Objects.equals(
                    appointment.businessId(),
                    authenticatedUser.getBusinessId()
                )
        ) {

            throw new AccessDeniedException(
                "La cita solicitada no pertenece "
                    + "al negocio de la sesión actual."
            );
        }
    }

    // =========================================================
    // TENANT DEFENCE · APPOINTMENT LIST
    // =========================================================

    /**
     * Evita que un listado pueda devolver accidentalmente
     * citas pertenecientes a otro tenant.
     */
    private void validateAppointmentListBusiness(

        AuthenticatedUser authenticatedUser,

        List<AppointmentResponse> appointments

    ) {

        requireCurrentBusinessAccess(
            authenticatedUser
        );

        if (
            appointments == null
                || appointments.isEmpty()
        ) {

            return;
        }

        for (
            AppointmentResponse appointment :
            appointments
        ) {

            if (
                appointment == null
            ) {

                throw new AccessDeniedException(
                    "La consulta contiene una cita no válida."
                );
            }

            if (
                appointment.businessId() == null
                    || !Objects.equals(
                        appointment.businessId(),
                        authenticatedUser.getBusinessId()
                    )
            ) {

                throw new AccessDeniedException(
                    "La consulta contiene citas pertenecientes "
                        + "a otro negocio."
                );
            }
        }
    }
}