package com.wacds.appointments.availability;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.availability.dto.ProfessionalAvailabilityRequest;
import com.wacds.appointments.availability.dto.ProfessionalAvailabilityResponse;
import com.wacds.appointments.error.ResourceNotFoundException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professional.ProfessionalRepository;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfessionalAvailabilityController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProfessionalAvailabilityService
        availabilityService;

    private final BusinessMembershipService
        businessMembershipService;

    private final ProfessionalRepository
        professionalRepository;

    // =========================================================
    // CREATE
    // =========================================================

    /**
     * Crea una franja de disponibilidad semanal
     * para un profesional.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     *
     * STAFF no puede modificar disponibilidad.
     *
     * Se mantienen dos rutas compatibles:
     *
     * /availability
     * /availabilities
     */
    @PostMapping({
        "/professionals/{professionalId}/availability",
        "/professionals/{professionalId}/availabilities"
    })
    public ResponseEntity<ProfessionalAvailabilityResponse>
        create(

            @PathVariable
            Long professionalId,

            @Valid
            @RequestBody
            ProfessionalAvailabilityRequest request,

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

        ProfessionalAvailabilityResponse response =
            availabilityService.create(
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
    // FIND ALL
    // =========================================================

    /**
     * Devuelve todas las franjas de disponibilidad
     * configuradas para un profesional.
     *
     * Incluye tanto activas como inactivas.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     *
     * STAFF queda excluido porque la disponibilidad semanal
     * forma parte de la configuración estructural
     * del profesional.
     */
    @GetMapping({
        "/professionals/{professionalId}/availability",
        "/professionals/{professionalId}/availabilities"
    })
    public ResponseEntity<
        List<ProfessionalAvailabilityResponse>
    > findAllByProfessional(

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

        List<ProfessionalAvailabilityResponse> response =
            availabilityService
                .findAllByProfessional(
                    professionalId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND ACTIVE
    // =========================================================

    /**
     * Devuelve únicamente las franjas activas
     * configuradas para el profesional.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     */
    @GetMapping({
        "/professionals/{professionalId}/availability/active",
        "/professionals/{professionalId}/availabilities/active"
    })
    public ResponseEntity<
        List<ProfessionalAvailabilityResponse>
    > findActiveByProfessional(

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

        List<ProfessionalAvailabilityResponse> response =
            availabilityService
                .findActiveByProfessional(
                    professionalId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    /**
     * Recupera una disponibilidad por id.
     *
     * Se mantienen ambas variantes legacy:
     *
     * /availability/{availabilityId}
     * /availabilities/{availabilityId}
     *
     * La capa de servicio continúa siendo responsable
     * de validar que la disponibilidad recuperada pertenece
     * al tenant autenticado.
     */
    @GetMapping({
        "/availability/{availabilityId}",
        "/availabilities/{availabilityId}"
    })
    public ResponseEntity<ProfessionalAvailabilityResponse>
        findById(

            @PathVariable
            Long availabilityId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateAvailabilityId(
            availabilityId
        );

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessManagement(
            authenticatedUser,
            businessId
        );

        ProfessionalAvailabilityResponse response =
            availabilityService
                .findById(
                    availabilityId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY PROFESSIONAL + ID
    // =========================================================

    /**
     * Recupera una disponibilidad concreta dentro
     * del contexto explícito de un profesional.
     */
    @GetMapping({
        "/professionals/{professionalId}/availability/{availabilityId}",
        "/professionals/{professionalId}/availabilities/{availabilityId}"
    })
    public ResponseEntity<ProfessionalAvailabilityResponse>
        findByProfessionalAndId(

            @PathVariable
            Long professionalId,

            @PathVariable
            Long availabilityId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateProfessionalId(
            professionalId
        );

        validateAvailabilityId(
            availabilityId
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

        ProfessionalAvailabilityResponse response =
            availabilityService
                .findByProfessionalAndId(
                    professionalId,
                    availabilityId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Elimina una disponibilidad semanal.
     *
     * Roles permitidos:
     *
     * - OWNER
     * - ADMIN
     *
     * ProfessionalAvailabilityService mantiene además
     * las validaciones internas de seguridad,
     * aislamiento multi-tenant y consistencia.
     */
    @DeleteMapping({
        "/professionals/{professionalId}/availability/{availabilityId}",
        "/professionals/{professionalId}/availabilities/{availabilityId}"
    })
    public ResponseEntity<Void>
        delete(

            @PathVariable
            Long professionalId,

            @PathVariable
            Long availabilityId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        validateProfessionalId(
            professionalId
        );

        validateAvailabilityId(
            availabilityId
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

        availabilityService.delete(
            professionalId,
            availabilityId
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    // =========================================================
    // SECURITY · CURRENT BUSINESS
    // =========================================================

    /**
     * Obtiene el Business activo de la sesión autenticada.
     *
     * El tenant nunca se deriva desde professionalId
     * ni desde availabilityId.
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
     * La disponibilidad semanal pertenece
     * a configuración estructural del Business.
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
     * Valida que el profesional exista dentro
     * del Business autenticado.
     *
     * Un professionalId perteneciente a otro tenant
     * se comporta como recurso inexistente.
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

    private void validateAvailabilityId(
        Long availabilityId
    ) {

        if (
            availabilityId == null
        ) {
            throw new IllegalArgumentException(
                "availabilityId es obligatorio."
            );
        }

        if (
            availabilityId <= 0
        ) {
            throw new IllegalArgumentException(
                "availabilityId debe ser mayor que 0."
            );
        }
    }
}