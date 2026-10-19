package com.wacds.appointments.scheduleexception;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;
import com.wacds.appointments.scheduleexception.dto.ScheduleExceptionRequest;
import com.wacds.appointments.scheduleexception.dto.ScheduleExceptionResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class ScheduleExceptionController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ScheduleExceptionService
        scheduleExceptionService;

    private final BusinessMembershipService
        businessMembershipService;

    private final ProfessionalRepository
        professionalRepository;

    // =========================================================
    // CREATE
    // =========================================================

    /**
     * Crea una excepción de horario para un profesional.
     *
     * Ejemplos:
     *
     * - día completo no disponible;
     * - horario especial;
     * - cambio puntual de jornada.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     */
    @PostMapping(
        "/api/v1/professionals/{professionalId}/schedule-exceptions"
    )
    public ResponseEntity<ScheduleExceptionResponse>
        create(

            @PathVariable
            Long professionalId,

            @Valid
            @RequestBody
            ScheduleExceptionRequest request,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateProfessionalId(
            professionalId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        requireProfessionalInBusiness(
            professionalId,
            businessId
        );

        ScheduleExceptionResponse response =
            scheduleExceptionService.create(
                professionalId,
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

    // =========================================================
    // FIND ALL BY PROFESSIONAL
    // =========================================================

    /**
     * Devuelve todas las excepciones de horario
     * configuradas para un profesional.
     *
     * Incluye activas e inactivas.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     */
    @GetMapping(
        "/api/v1/professionals/{professionalId}/schedule-exceptions"
    )
    public ResponseEntity<List<ScheduleExceptionResponse>>
        findAllByProfessional(

            @PathVariable
            Long professionalId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateProfessionalId(
            professionalId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        requireProfessionalInBusiness(
            professionalId,
            businessId
        );

        List<ScheduleExceptionResponse> response =
            scheduleExceptionService
                .findAllByProfessional(
                    professionalId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND ACTIVE BY PROFESSIONAL
    // =========================================================

    /**
     * Devuelve únicamente las excepciones activas
     * de un profesional.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     */
    @GetMapping(
        "/api/v1/professionals/{professionalId}/schedule-exceptions/active"
    )
    public ResponseEntity<List<ScheduleExceptionResponse>>
        findActiveByProfessional(

            @PathVariable
            Long professionalId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateProfessionalId(
            professionalId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        requireProfessionalInBusiness(
            professionalId,
            businessId
        );

        List<ScheduleExceptionResponse> response =
            scheduleExceptionService
                .findActiveByProfessional(
                    professionalId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND ACTIVE BY PROFESSIONAL + DATE
    // =========================================================

    /**
     * Devuelve las excepciones activas de un profesional
     * para una fecha concreta.
     *
     * Ejemplo:
     *
     * GET
     * /api/v1/professionals/4/schedule-exceptions/date/2026-10-10
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     */
    @GetMapping(
        "/api/v1/professionals/{professionalId}/schedule-exceptions/date/{date}"
    )
    public ResponseEntity<List<ScheduleExceptionResponse>>
        findActiveByProfessionalAndDate(

            @PathVariable
            Long professionalId,

            @PathVariable
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateProfessionalId(
            professionalId
        );

        validateDate(
            date
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        requireProfessionalInBusiness(
            professionalId,
            businessId
        );

        List<ScheduleExceptionResponse> response =
            scheduleExceptionService
                .findActiveByProfessionalAndDate(
                    professionalId,
                    date
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    /**
     * Recupera una excepción de horario por id.
     *
     * La validación final de pertenencia al tenant
     * debe mantenerse también dentro del service.
     */
    @GetMapping(
        "/api/v1/schedule-exceptions/{scheduleExceptionId}"
    )
    public ResponseEntity<ScheduleExceptionResponse>
        findById(

            @PathVariable
            Long scheduleExceptionId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateScheduleExceptionId(
            scheduleExceptionId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        ScheduleExceptionResponse response =
            scheduleExceptionService
                .findById(
                    scheduleExceptionId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY PROFESSIONAL + ID
    // =========================================================

    /**
     * Recupera una excepción concreta dentro
     * del contexto de un profesional.
     */
    @GetMapping(
        "/api/v1/professionals/{professionalId}/schedule-exceptions/{scheduleExceptionId}"
    )
    public ResponseEntity<ScheduleExceptionResponse>
        findByProfessionalAndId(

            @PathVariable
            Long professionalId,

            @PathVariable
            Long scheduleExceptionId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateProfessionalId(
            professionalId
        );

        validateScheduleExceptionId(
            scheduleExceptionId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        requireProfessionalInBusiness(
            professionalId,
            businessId
        );

        ScheduleExceptionResponse response =
            scheduleExceptionService
                .findByProfessionalAndId(
                    professionalId,
                    scheduleExceptionId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Elimina una excepción de horario.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     *
     * Como esta operación cambia la agenda,
     * ScheduleExceptionService debe mantener también
     * la autorización tenant-safe y el lock de agenda.
     */
    @DeleteMapping(
        "/api/v1/professionals/{professionalId}/schedule-exceptions/{scheduleExceptionId}"
    )
    public ResponseEntity<Void>
        delete(

            @PathVariable
            Long professionalId,

            @PathVariable
            Long scheduleExceptionId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateProfessionalId(
            professionalId
        );

        validateScheduleExceptionId(
            scheduleExceptionId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        requireProfessionalInBusiness(
            professionalId,
            businessId
        );

        scheduleExceptionService.delete(
            professionalId,
            scheduleExceptionId
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    // =========================================================
    // SECURITY · CURRENT BUSINESS
    // =========================================================

    /**
     * Obtiene el Business de la sesión autenticada.
     *
     * El tenant nunca se deriva del professionalId
     * ni del scheduleExceptionId.
     */
    private Long requireCurrentBusinessId(
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

        Long businessId =
            authenticatedUser.getBusinessId();

        if (
            businessId <= 0
        ) {
            throw new AccessDeniedException(
                "El businessId de la sesión no es válido."
            );
        }

        return businessId;
    }

    /**
     * Las excepciones de agenda forman parte
     * de la configuración estructural del Business.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     */
    private void requireBusinessManagement(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );
    }

    // =========================================================
    // TENANT-SAFE PROFESSIONAL LOOKUP
    // =========================================================

    /**
     * Valida que el profesional pertenezca
     * exactamente al Business autenticado.
     *
     * Un professionalId de otro tenant
     * se trata como recurso inexistente.
     */
    private Professional requireProfessionalInBusiness(
        Long professionalId,
        Long businessId
    ) {

        Professional professional =
            professionalRepository
                .findById(
                    professionalId
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "No existe ningún profesional con id: "
                            + professionalId
                    )
                );

        if (
            professional.getBusiness() == null
                || professional.getBusiness().getId() == null
                || !Objects.equals(
                    professional.getBusiness().getId(),
                    businessId
                )
        ) {
            throw new ResourceNotFoundException(
                "No existe el profesional "
                    + professionalId
                    + " dentro del negocio actual."
            );
        }

        return professional;
    }

    // =========================================================
    // DATE VALIDATION
    // =========================================================

    private void validateDate(
        LocalDate date
    ) {

        if (
            date == null
        ) {
            throw new IllegalArgumentException(
                "La fecha es obligatoria."
            );
        }
    }

    // =========================================================
    // ID VALIDATION
    // =========================================================

    private void validateProfessionalId(
        Long professionalId
    ) {

        if (
            professionalId == null
        ) {
            throw new IllegalArgumentException(
                "professionalId es obligatorio."
            );
        }

        if (
            professionalId <= 0
        ) {
            throw new IllegalArgumentException(
                "professionalId debe ser mayor que 0."
            );
        }
    }

    private void validateScheduleExceptionId(
        Long scheduleExceptionId
    ) {

        if (
            scheduleExceptionId == null
        ) {
            throw new IllegalArgumentException(
                "scheduleExceptionId es obligatorio."
            );
        }

        if (
            scheduleExceptionId <= 0
        ) {
            throw new IllegalArgumentException(
                "scheduleExceptionId debe ser mayor que 0."
            );
        }
    }
}