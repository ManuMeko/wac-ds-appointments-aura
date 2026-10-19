package com.wacds.appointments.professional;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.professional.dto.ProfessionalRequest;
import com.wacds.appointments.professional.dto.ProfessionalResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class ProfessionalController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ProfessionalService professionalService;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // CREATE · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Crea un profesional dentro de un negocio concreto.
     *
     * Ruta mantenida por compatibilidad.
     */
    @PostMapping(
        "/api/v1/businesses/{businessId}/professionals"
    )
    public ResponseEntity<ProfessionalResponse> create(

        @PathVariable
        Long businessId,

        @Valid
        @RequestBody
        ProfessionalRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser
    ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService.create(
                businessId,
                request
            );

        validateProfessionalBusiness(
            authenticatedUser,
            response
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
    // CREATE · CURRENT BUSINESS
    // =========================================================

    /**
     * Crea un profesional dentro del negocio activo
     * de la sesión Business.
     *
     * Endpoint recomendado para el frontend.
     */
    @PostMapping(
        "/api/v1/professionals"
    )
    public ResponseEntity<ProfessionalResponse>
        createForCurrentBusiness(

            @Valid
            @RequestBody
            ProfessionalRequest request,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService.create(
                businessId,
                request
            );

        validateProfessionalBusiness(
            authenticatedUser,
            response
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
    // UPDATE · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Edita un profesional dentro de un negocio concreto.
     */
    @PutMapping(
        "/api/v1/businesses/{businessId}/professionals/{professionalId}"
    )
    public ResponseEntity<ProfessionalResponse> update(

        @PathVariable
        Long businessId,

        @PathVariable
        Long professionalId,

        @Valid
        @RequestBody
        ProfessionalRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser
    ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService.update(
                businessId,
                professionalId,
                request
            );

        validateProfessionalBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // UPDATE · CURRENT BUSINESS
    // =========================================================

    /**
     * Edita un profesional del negocio actual.
     *
     * PUT /api/v1/professionals/{professionalId}
     */
    @PutMapping(
        "/api/v1/professionals/{professionalId}"
    )
    public ResponseEntity<ProfessionalResponse>
        updateForCurrentBusiness(

            @PathVariable
            Long professionalId,

            @Valid
            @RequestBody
            ProfessionalRequest request,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService.update(
                businessId,
                professionalId,
                request
            );

        validateProfessionalBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // ACTIVE STATUS · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Activa o desactiva un profesional.
     */
    @PatchMapping(
        "/api/v1/businesses/{businessId}/professionals/{professionalId}/active"
    )
    public ResponseEntity<ProfessionalResponse>
        setActive(

            @PathVariable
            Long businessId,

            @PathVariable
            Long professionalId,

            @RequestParam
            boolean active,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService.setActive(
                businessId,
                professionalId,
                active
            );

        validateProfessionalBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // ACTIVE STATUS · CURRENT BUSINESS
    // =========================================================

    /**
     * Activa o desactiva un profesional
     * del negocio actual.
     *
     * Ejemplos:
     *
     * PATCH /api/v1/professionals/1/active?active=false
     * PATCH /api/v1/professionals/1/active?active=true
     */
    @PatchMapping(
        "/api/v1/professionals/{professionalId}/active"
    )
    public ResponseEntity<ProfessionalResponse>
        setActiveForCurrentBusiness(

            @PathVariable
            Long professionalId,

            @RequestParam
            boolean active,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService.setActive(
                businessId,
                professionalId,
                active
            );

        validateProfessionalBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // BOOKING ENABLED · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Habilita o deshabilita nuevas reservas
     * para un profesional.
     */
    @PatchMapping(
        "/api/v1/businesses/{businessId}/professionals/{professionalId}/booking-enabled"
    )
    public ResponseEntity<ProfessionalResponse>
        setBookingEnabled(

            @PathVariable
            Long businessId,

            @PathVariable
            Long professionalId,

            @RequestParam
            boolean enabled,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService.setBookingEnabled(
                businessId,
                professionalId,
                enabled
            );

        validateProfessionalBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // BOOKING ENABLED · CURRENT BUSINESS
    // =========================================================

    /**
     * Habilita o deshabilita nuevas reservas
     * para un profesional del negocio actual.
     *
     * Ejemplos:
     *
     * PATCH /api/v1/professionals/1/booking-enabled?enabled=false
     * PATCH /api/v1/professionals/1/booking-enabled?enabled=true
     */
    @PatchMapping(
        "/api/v1/professionals/{professionalId}/booking-enabled"
    )
    public ResponseEntity<ProfessionalResponse>
        setBookingEnabledForCurrentBusiness(

            @PathVariable
            Long professionalId,

            @RequestParam
            boolean enabled,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService.setBookingEnabled(
                businessId,
                professionalId,
                enabled
            );

        validateProfessionalBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // LIST ALL · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Devuelve todos los profesionales de un negocio.
     *
     * Ruta mantenida por compatibilidad.
     */
    @GetMapping(
        "/api/v1/businesses/{businessId}/professionals"
    )
    public ResponseEntity<List<ProfessionalResponse>>
        findAllByBusiness(

            @PathVariable
            Long businessId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        List<ProfessionalResponse> response =
            professionalService
                .findAllByBusiness(
                    businessId
                );

        validateProfessionalListBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // LIST ALL · CURRENT BUSINESS
    // =========================================================

    /**
     * Devuelve todos los profesionales pertenecientes
     * al negocio activo de la sesión.
     *
     * Endpoint recomendado para el panel Business.
     */
    @GetMapping(
        "/api/v1/professionals"
    )
    public ResponseEntity<List<ProfessionalResponse>>
        findAllForCurrentBusiness(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        List<ProfessionalResponse> response =
            professionalService
                .findAllByBusiness(
                    businessId
                );

        validateProfessionalListBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // BOOKABLE · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Devuelve los profesionales disponibles
     * para reservas dentro de un negocio.
     */
    @GetMapping(
        "/api/v1/businesses/{businessId}/professionals/bookable"
    )
    public ResponseEntity<List<ProfessionalResponse>>
        findBookableByBusiness(

            @PathVariable
            Long businessId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        List<ProfessionalResponse> response =
            professionalService
                .findBookableByBusiness(
                    businessId
                );

        validateProfessionalListBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // BOOKABLE · CURRENT BUSINESS
    // =========================================================

    /**
     * Devuelve únicamente profesionales:
     *
     * active = true
     * bookingEnabled = true
     *
     * dentro del negocio actual.
     */
    @GetMapping(
        "/api/v1/professionals/bookable"
    )
    public ResponseEntity<List<ProfessionalResponse>>
        findBookableForCurrentBusiness(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        List<ProfessionalResponse> response =
            professionalService
                .findBookableByBusiness(
                    businessId
                );

        validateProfessionalListBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY ID · CURRENT BUSINESS
    // =========================================================

    /**
     * Devuelve un profesional por ID asegurando
     * que pertenece al negocio activo.
     */
    @GetMapping(
        "/api/v1/professionals/{professionalId}"
    )
    public ResponseEntity<ProfessionalResponse> findById(

        @PathVariable
        Long professionalId,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser
    ) {

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService
                .findByBusinessAndId(
                    businessId,
                    professionalId
                );

        validateProfessionalBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY BUSINESS AND ID · LEGACY ROUTE
    // =========================================================

    @GetMapping(
        "/api/v1/businesses/{businessId}/professionals/{professionalId}"
    )
    public ResponseEntity<ProfessionalResponse>
        findByBusinessAndId(

            @PathVariable
            Long businessId,

            @PathVariable
            Long professionalId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ProfessionalResponse response =
            professionalService
                .findByBusinessAndId(
                    businessId,
                    professionalId
                );

        validateProfessionalBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // SECURITY · CURRENT BUSINESS
    // =========================================================

    /**
     * Obtiene el businessId activo almacenado
     * en el principal autenticado.
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

        return authenticatedUser
            .getBusinessId();
    }

    // =========================================================
    // SECURITY · BUSINESS ACCESS
    // =========================================================

    /**
     * Comprueba que el usuario autenticado
     * tiene acceso al negocio solicitado.
     *
     * La autorización estructural OWNER / ADMIN
     * sigue ejecutándose en ProfessionalService.
     */
    private void requireBusinessAccess(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        if (
            authenticatedUser == null
                || authenticatedUser.getBusinessId() == null
        ) {
            throw new AccessDeniedException(
                "No existe una sesión Business válida."
            );
        }

        if (
            businessId == null
                || businessId <= 0
        ) {
            throw new AccessDeniedException(
                "El negocio solicitado no es válido."
            );
        }

        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );
    }

    // =========================================================
    // TENANT DEFENCE · SINGLE RESPONSE
    // =========================================================

    /**
     * Comprueba defensivamente que el profesional
     * pertenece al mismo Business de la sesión.
     */
    private void validateProfessionalBusiness(
        AuthenticatedUser authenticatedUser,
        ProfessionalResponse professional
    ) {

        Long currentBusinessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        if (
            professional == null
        ) {
            throw new AccessDeniedException(
                "No se puede validar el acceso al profesional."
            );
        }

        if (
            professional.businessId() == null
                || !Objects.equals(
                    professional.businessId(),
                    currentBusinessId
                )
        ) {
            throw new AccessDeniedException(
                "El profesional solicitado no pertenece "
                    + "al negocio de la sesión actual."
            );
        }
    }

    // =========================================================
    // TENANT DEFENCE · LIST
    // =========================================================

    /**
     * Ningún ProfessionalResponse perteneciente
     * a otro tenant puede abandonar esta capa.
     */
    private void validateProfessionalListBusiness(
        AuthenticatedUser authenticatedUser,
        List<ProfessionalResponse> professionals
    ) {

        Long currentBusinessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        if (
            professionals == null
                || professionals.isEmpty()
        ) {
            return;
        }

        for (
            ProfessionalResponse professional
                : professionals
        ) {

            if (
                professional == null
                    || professional.businessId() == null
                    || !Objects.equals(
                        professional.businessId(),
                        currentBusinessId
                    )
            ) {
                throw new AccessDeniedException(
                    "La consulta de profesionales contiene "
                        + "datos ajenos al negocio "
                        + "de la sesión actual."
                );
            }
        }
    }
}