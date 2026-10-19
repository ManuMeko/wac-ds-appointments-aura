package com.wacds.appointments.customer;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.customer.dto.CustomerRequest;
import com.wacds.appointments.customer.dto.CustomerResponse;

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
public class CustomerController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final CustomerService customerService;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // CREATE · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Crea un cliente dentro de un Business concreto.
     *
     * Ruta legacy mantenida por compatibilidad.
     */
    @PostMapping(
        "/api/v1/businesses/{businessId}/customers"
    )
    public ResponseEntity<CustomerResponse> create(

        @PathVariable
        Long businessId,

        @Valid
        @RequestBody
        CustomerRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser
    ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        CustomerResponse response =
            customerService.create(
                businessId,
                request
            );

        validateCustomerBusiness(
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
     * Crea un cliente dentro del Business
     * perteneciente a la sesión actual.
     *
     * Endpoint recomendado para el frontend Business.
     *
     * POST /api/v1/customers
     */
    @PostMapping(
        "/api/v1/customers"
    )
    public ResponseEntity<CustomerResponse>
        createForCurrentBusiness(

            @Valid
            @RequestBody
            CustomerRequest request,

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

        CustomerResponse response =
            customerService.create(
                businessId,
                request
            );

        validateCustomerBusiness(
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
     * Actualiza un cliente dentro de un Business
     * indicado explícitamente.
     *
     * Ruta legacy mantenida por compatibilidad.
     */
    @PutMapping(
        "/api/v1/businesses/{businessId}/customers/{customerId}"
    )
    public ResponseEntity<CustomerResponse> update(

        @PathVariable
        Long businessId,

        @PathVariable
        Long customerId,

        @Valid
        @RequestBody
        CustomerRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser
    ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        CustomerResponse response =
            customerService.update(
                businessId,
                customerId,
                request
            );

        validateCustomerBusiness(
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
     * Actualiza un cliente del tenant autenticado.
     *
     * Endpoint recomendado para el frontend Business.
     *
     * PUT /api/v1/customers/{customerId}
     */
    @PutMapping(
        "/api/v1/customers/{customerId}"
    )
    public ResponseEntity<CustomerResponse>
        updateForCurrentBusiness(

            @PathVariable
            Long customerId,

            @Valid
            @RequestBody
            CustomerRequest request,

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

        CustomerResponse response =
            customerService.update(
                businessId,
                customerId,
                request
            );

        validateCustomerBusiness(
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
     * Activa o desactiva un cliente dentro
     * de un Business concreto.
     *
     * Ruta legacy mantenida por compatibilidad.
     */
    @PatchMapping(
        "/api/v1/businesses/{businessId}/customers/{customerId}/active"
    )
    public ResponseEntity<CustomerResponse>
        setActive(

            @PathVariable
            Long businessId,

            @PathVariable
            Long customerId,

            @RequestParam
            boolean active,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        CustomerResponse response =
            customerService.setActive(
                businessId,
                customerId,
                active
            );

        validateCustomerBusiness(
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
     * Activa o desactiva un cliente del tenant actual.
     *
     * Ejemplos:
     *
     * PATCH /api/v1/customers/12/active?active=false
     * PATCH /api/v1/customers/12/active?active=true
     */
    @PatchMapping(
        "/api/v1/customers/{customerId}/active"
    )
    public ResponseEntity<CustomerResponse>
        setActiveForCurrentBusiness(

            @PathVariable
            Long customerId,

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

        CustomerResponse response =
            customerService.setActive(
                businessId,
                customerId,
                active
            );

        validateCustomerBusiness(
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
     * Lista todos los clientes de un Business.
     *
     * Ruta legacy.
     */
    @GetMapping(
        "/api/v1/businesses/{businessId}/customers"
    )
    public ResponseEntity<List<CustomerResponse>>
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

        List<CustomerResponse> customers =
            customerService
                .findAllByBusiness(
                    businessId
                );

        validateCustomerListBusiness(
            authenticatedUser,
            customers
        );

        return ResponseEntity.ok(
            customers
        );
    }

    // =========================================================
    // LIST ALL · CURRENT BUSINESS
    // =========================================================

    /**
     * Lista todos los clientes del tenant actual.
     *
     * GET /api/v1/customers
     */
    @GetMapping(
        "/api/v1/customers"
    )
    public ResponseEntity<List<CustomerResponse>>
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

        List<CustomerResponse> customers =
            customerService
                .findAllByBusiness(
                    businessId
                );

        validateCustomerListBusiness(
            authenticatedUser,
            customers
        );

        return ResponseEntity.ok(
            customers
        );
    }

    // =========================================================
    // ACTIVE · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Lista clientes activos de un Business.
     *
     * Ruta legacy.
     */
    @GetMapping(
        "/api/v1/businesses/{businessId}/customers/active"
    )
    public ResponseEntity<List<CustomerResponse>>
        findActiveByBusiness(

            @PathVariable
            Long businessId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        List<CustomerResponse> customers =
            customerService
                .findActiveByBusiness(
                    businessId
                );

        validateCustomerListBusiness(
            authenticatedUser,
            customers
        );

        return ResponseEntity.ok(
            customers
        );
    }

    // =========================================================
    // ACTIVE · CURRENT BUSINESS
    // =========================================================

    /**
     * Lista únicamente los clientes activos
     * del Business autenticado.
     *
     * GET /api/v1/customers/active
     */
    @GetMapping(
        "/api/v1/customers/active"
    )
    public ResponseEntity<List<CustomerResponse>>
        findActiveForCurrentBusiness(

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

        List<CustomerResponse> customers =
            customerService
                .findActiveByBusiness(
                    businessId
                );

        validateCustomerListBusiness(
            authenticatedUser,
            customers
        );

        return ResponseEntity.ok(
            customers
        );
    }

    // =========================================================
    // FIND BY ID · CURRENT BUSINESS
    // =========================================================

    /**
     * Obtiene un cliente concreto del Business autenticado.
     *
     * GET /api/v1/customers/{customerId}
     *
     * El Business nunca se deriva del customerId.
     *
     * Primero se obtiene el tenant de la sesión y después
     * se ejecuta una búsqueda tenant-safe por:
     *
     * businessId + customerId
     *
     * Este endpoint será la base de:
     *
     * /business/customers/[id]
     */
    @GetMapping(
        "/api/v1/customers/{customerId}"
    )
    public ResponseEntity<CustomerResponse>
        findById(

            @PathVariable
            Long customerId,

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

        CustomerResponse response =
            customerService
                .findByBusinessAndId(
                    businessId,
                    customerId
                );

        validateCustomerBusiness(
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

    /**
     * Obtiene un cliente concreto dentro
     * de un Business indicado explícitamente.
     *
     * Ruta legacy mantenida por compatibilidad.
     */
    @GetMapping(
        "/api/v1/businesses/{businessId}/customers/{customerId}"
    )
    public ResponseEntity<CustomerResponse>
        findByBusinessAndId(

            @PathVariable
            Long businessId,

            @PathVariable
            Long customerId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        CustomerResponse response =
            customerService
                .findByBusinessAndId(
                    businessId,
                    customerId
                );

        validateCustomerBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY EMAIL · LEGACY BUSINESS-ID ROUTE
    // =========================================================

    /**
     * Busca un cliente por email dentro
     * de un Business concreto.
     *
     * Ruta legacy.
     */
    @GetMapping(
        "/api/v1/businesses/{businessId}/customers/by-email"
    )
    public ResponseEntity<CustomerResponse>
        findByBusinessAndEmail(

            @PathVariable
            Long businessId,

            @RequestParam
            String email,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        requireBusinessAccess(
            authenticatedUser,
            businessId
        );

        CustomerResponse response =
            customerService
                .findByBusinessAndEmail(
                    businessId,
                    email
                );

        validateCustomerBusiness(
            authenticatedUser,
            response
        );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND BY EMAIL · CURRENT BUSINESS
    // =========================================================

    /**
     * Busca un cliente por email dentro
     * del tenant autenticado.
     *
     * GET /api/v1/customers/by-email?email=...
     */
    @GetMapping(
        "/api/v1/customers/by-email"
    )
    public ResponseEntity<CustomerResponse>
        findByEmailForCurrentBusiness(

            @RequestParam
            String email,

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

        CustomerResponse response =
            customerService
                .findByBusinessAndEmail(
                    businessId,
                    email
                );

        validateCustomerBusiness(
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
     * Obtiene el businessId del principal autenticado.
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
     * Valida acceso al Business actualmente
     * incluido en la sesión.
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
     * Segunda barrera multi-tenant del controller.
     *
     * BusinessMembershipService vuelve a comprobar
     * la relación persistida entre:
     *
     * - usuario;
     * - Business;
     * - membership;
     * - rol;
     * - estado.
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
    // TENANT DEFENCE · SINGLE CUSTOMER
    // =========================================================

    /**
     * Comprueba defensivamente que un CustomerResponse
     * pertenece exactamente al tenant autenticado.
     */
    private void validateCustomerBusiness(
        AuthenticatedUser authenticatedUser,
        CustomerResponse customer
    ) {

        Long currentBusinessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        if (
            customer == null
        ) {
            throw new AccessDeniedException(
                "No se puede validar el acceso al cliente."
            );
        }

        if (
            customer.businessId() == null
                || !Objects.equals(
                    customer.businessId(),
                    currentBusinessId
                )
        ) {
            throw new AccessDeniedException(
                "El cliente solicitado no pertenece "
                    + "al negocio de la sesión actual."
            );
        }
    }

    // =========================================================
    // TENANT DEFENCE · CUSTOMER LIST
    // =========================================================

    /**
     * Defensa adicional frente a una posible consulta
     * mal filtrada dentro de la capa de persistencia.
     *
     * Ningún listado devuelto al frontend puede contener
     * clientes pertenecientes a otro Business.
     */
    private void validateCustomerListBusiness(
        AuthenticatedUser authenticatedUser,
        List<CustomerResponse> customers
    ) {

        Long currentBusinessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        if (
            customers == null
                || customers.isEmpty()
        ) {
            return;
        }

        for (
            CustomerResponse customer
                : customers
        ) {

            if (
                customer == null
                    || customer.businessId() == null
                    || !Objects.equals(
                        customer.businessId(),
                        currentBusinessId
                    )
            ) {
                throw new AccessDeniedException(
                    "La consulta de clientes contiene "
                        + "datos ajenos al negocio "
                        + "de la sesión actual."
                );
            }
        }
    }
}