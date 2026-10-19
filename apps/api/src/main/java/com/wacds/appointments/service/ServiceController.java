package com.wacds.appointments.service;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.service.dto.ServiceRequest;
import com.wacds.appointments.service.dto.ServiceResponse;

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
public class ServiceController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ServiceService serviceService;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // CREATE · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Crea un servicio dentro de un negocio concreto.
     *
     * Ruta mantenida por compatibilidad.
     */
    @PostMapping(
        "/api/v1/businesses/{businessId}/services"
    )
    public ResponseEntity<ServiceResponse> create(

        @PathVariable
        Long businessId,

        @Valid
        @RequestBody
        ServiceRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser
    ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ServiceResponse response =
            serviceService.create(
                businessId,
                request
            );

        validateServiceBusiness(
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
     * Crea un servicio dentro del negocio activo
     * de la sesión JWT.
     *
     * Endpoint recomendado para Business.
     */
    @PostMapping(
        "/api/v1/services"
    )
    public ResponseEntity<ServiceResponse>
        createForCurrentBusiness(

            @Valid
            @RequestBody
            ServiceRequest request,

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

        ServiceResponse response =
            serviceService.create(
                businessId,
                request
            );

        validateServiceBusiness(
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
     * Edita un servicio dentro de un negocio concreto.
     */
    @PutMapping(
        "/api/v1/businesses/{businessId}/services/{serviceId}"
    )
    public ResponseEntity<ServiceResponse> update(

        @PathVariable
        Long businessId,

        @PathVariable
        Long serviceId,

        @Valid
        @RequestBody
        ServiceRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser
    ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ServiceResponse response =
            serviceService.update(
                businessId,
                serviceId,
                request
            );

        validateServiceBusiness(
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
     * Edita un servicio del negocio actual.
     *
     * PUT /api/v1/services/{serviceId}
     */
    @PutMapping(
        "/api/v1/services/{serviceId}"
    )
    public ResponseEntity<ServiceResponse>
        updateForCurrentBusiness(

            @PathVariable
            Long serviceId,

            @Valid
            @RequestBody
            ServiceRequest request,

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

        ServiceResponse response =
            serviceService.update(
                businessId,
                serviceId,
                request
            );

        validateServiceBusiness(
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
     * Activa o desactiva un servicio.
     */
    @PatchMapping(
        "/api/v1/businesses/{businessId}/services/{serviceId}/active"
    )
    public ResponseEntity<ServiceResponse>
        setActive(

            @PathVariable
            Long businessId,

            @PathVariable
            Long serviceId,

            @RequestParam
            boolean active,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ServiceResponse response =
            serviceService.setActive(
                businessId,
                serviceId,
                active
            );

        validateServiceBusiness(
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
     * Activa o desactiva un servicio del negocio actual.
     *
     * Ejemplos:
     *
     * PATCH /api/v1/services/1/active?active=false
     * PATCH /api/v1/services/1/active?active=true
     */
    @PatchMapping(
        "/api/v1/services/{serviceId}/active"
    )
    public ResponseEntity<ServiceResponse>
        setActiveForCurrentBusiness(

            @PathVariable
            Long serviceId,

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

        ServiceResponse response =
            serviceService.setActive(
                businessId,
                serviceId,
                active
            );

        validateServiceBusiness(
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
     * para un servicio.
     */
    @PatchMapping(
        "/api/v1/businesses/{businessId}/services/{serviceId}/booking-enabled"
    )
    public ResponseEntity<ServiceResponse>
        setBookingEnabled(

            @PathVariable
            Long businessId,

            @PathVariable
            Long serviceId,

            @RequestParam
            boolean enabled,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ServiceResponse response =
            serviceService.setBookingEnabled(
                businessId,
                serviceId,
                enabled
            );

        validateServiceBusiness(
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
     * para un servicio del negocio actual.
     *
     * Ejemplos:
     *
     * PATCH /api/v1/services/1/booking-enabled?enabled=false
     * PATCH /api/v1/services/1/booking-enabled?enabled=true
     */
    @PatchMapping(
        "/api/v1/services/{serviceId}/booking-enabled"
    )
    public ResponseEntity<ServiceResponse>
        setBookingEnabledForCurrentBusiness(

            @PathVariable
            Long serviceId,

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

        ServiceResponse response =
            serviceService.setBookingEnabled(
                businessId,
                serviceId,
                enabled
            );

        validateServiceBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // LIST · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Devuelve todos los servicios de un negocio concreto.
     */
    @GetMapping(
        "/api/v1/businesses/{businessId}/services"
    )
    public ResponseEntity<List<ServiceResponse>>
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

        List<ServiceResponse> response =
            serviceService
                .findAllByBusiness(
                    businessId
                );

        validateServiceListBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // LIST · CURRENT BUSINESS
    // =========================================================

    /**
     * Devuelve todos los servicios del negocio
     * activo de la sesión.
     */
    @GetMapping(
        "/api/v1/services"
    )
    public ResponseEntity<List<ServiceResponse>>
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

        List<ServiceResponse> response =
            serviceService
                .findAllByBusiness(
                    businessId
                );

        validateServiceListBusiness(
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
     * Devuelve un servicio por ID dentro
     * del tenant actual.
     */
    @GetMapping(
        "/api/v1/services/{serviceId}"
    )
    public ResponseEntity<ServiceResponse> findById(

        @PathVariable
        Long serviceId,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser
    ) {

        requireCurrentBusinessAccess(
            authenticatedUser
        );

        ServiceResponse response =
            serviceService.findById(
                serviceId
            );

        validateServiceBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY ID · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Busca un servicio mediante businessId + serviceId.
     */
    @GetMapping(
        "/api/v1/businesses/{businessId}/services/{serviceId}"
    )
    public ResponseEntity<ServiceResponse>
        findByBusinessAndId(

            @PathVariable
            Long businessId,

            @PathVariable
            Long serviceId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ServiceResponse response =
            serviceService
                .findByBusinessAndId(
                    businessId,
                    serviceId
                );

        validateServiceBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY SLUG · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    @GetMapping(
        "/api/v1/businesses/{businessId}/services/slug/{slug}"
    )
    public ResponseEntity<ServiceResponse>
        findByBusinessAndSlug(

            @PathVariable
            Long businessId,

            @PathVariable
            String slug,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        ServiceResponse response =
            serviceService
                .findByBusinessAndSlug(
                    businessId,
                    slug
                );

        validateServiceBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY SLUG · CURRENT BUSINESS
    // =========================================================

    /**
     * Busca un servicio por slug dentro
     * del negocio autenticado.
     */
    @GetMapping(
        "/api/v1/services/slug/{slug}"
    )
    public ResponseEntity<ServiceResponse>
        findBySlugForCurrentBusiness(

            @PathVariable
            String slug,

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

        ServiceResponse response =
            serviceService
                .findByBusinessAndSlug(
                    businessId,
                    slug
                );

        validateServiceBusiness(
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
     * Obtiene el businessId almacenado
     * en la sesión autenticada.
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

    /**
     * Comprueba que la sesión puede acceder
     * al negocio actual.
     */
    private void requireCurrentBusinessAccess(
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
    }

    /**
     * Valida acceso general al Business.
     *
     * La autorización estructural OWNER/ADMIN
     * continúa aplicándose dentro de ServiceService
     * mediante requireBusinessManagement(...).
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
     * Comprueba defensivamente que la respuesta
     * pertenece al mismo Business que la sesión.
     */
    private void validateServiceBusiness(
        AuthenticatedUser authenticatedUser,
        ServiceResponse service
    ) {

        Long currentBusinessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        if (
            service == null
        ) {
            throw new AccessDeniedException(
                "No se puede validar el acceso al servicio."
            );
        }

        if (
            service.businessId() == null
                || !Objects.equals(
                    service.businessId(),
                    currentBusinessId
                )
        ) {
            throw new AccessDeniedException(
                "El servicio solicitado no pertenece "
                    + "al negocio de la sesión actual."
            );
        }
    }

    // =========================================================
    // TENANT DEFENCE · LIST
    // =========================================================

    /**
     * Ningún ServiceResponse de otro tenant
     * puede abandonar esta capa.
     */
    private void validateServiceListBusiness(
        AuthenticatedUser authenticatedUser,
        List<ServiceResponse> services
    ) {

        Long currentBusinessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        if (
            services == null
                || services.isEmpty()
        ) {
            return;
        }

        for (
            ServiceResponse service
                : services
        ) {

            if (
                service == null
                    || service.businessId() == null
                    || !Objects.equals(
                        service.businessId(),
                        currentBusinessId
                    )
            ) {
                throw new AccessDeniedException(
                    "La consulta de servicios contiene "
                        + "datos ajenos al negocio "
                        + "de la sesión actual."
                );
            }
        }
    }
}