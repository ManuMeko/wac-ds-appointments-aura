package com.wacds.appointments.business;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.business.dto.BusinessResponse;
import com.wacds.appointments.business.dto.BusinessUpdateRequest;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/v1/businesses"
)
@RequiredArgsConstructor
public class BusinessController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessService
        businessService;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // GET BUSINESS PROFILE · LEGACY / EXPLICIT BUSINESS ID
    // =========================================================

    /**
     * Devuelve el perfil completo de un Business concreto.
     *
     * Permitidos:
     *
     * OWNER
     * ADMIN
     * STAFF
     *
     * Seguridad:
     *
     * - businessId debe pertenecer al tenant autenticado;
     * - la membership debe seguir activa;
     * - el usuario debe seguir activo;
     * - el Business debe seguir activo;
     * - el rol persistido debe coincidir con la sesión.
     *
     * Esta ruta se conserva para compatibilidad.
     */
    @GetMapping(
        "/{businessId}"
    )
    public ResponseEntity<BusinessResponse>
        findById(

            @PathVariable
            Long businessId
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        /*
         * OWNER / ADMIN / STAFF.
         *
         * La autorización se comprueba contra los datos
         * persistidos y no únicamente contra el JWT.
         */
        businessMembershipService
            .requireOperationalAccess(
                authenticatedUser,
                businessId
            );

        BusinessResponse response =
            businessService.findById(
                businessId
            );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // GET BUSINESS PROFILE · CURRENT BUSINESS
    // =========================================================

    /**
     * Devuelve el perfil del Business perteneciente
     * a la sesión autenticada.
     *
     * Endpoint recomendado para el frontend Business.
     *
     * GET /api/v1/businesses/current
     *
     * Permitidos:
     *
     * OWNER
     * ADMIN
     * STAFF
     *
     * El frontend no proporciona businessId.
     * Se obtiene directamente del principal JWT.
     */
    @GetMapping(
        "/current"
    )
    public ResponseEntity<BusinessResponse>
        findCurrentBusiness() {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        businessMembershipService
            .requireOperationalAccess(
                authenticatedUser,
                businessId
            );

        BusinessResponse response =
            businessService.findById(
                businessId
            );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // UPDATE BUSINESS PROFILE · LEGACY / EXPLICIT BUSINESS ID
    // =========================================================

    /**
     * Actualiza parcialmente un Business concreto.
     *
     * Permitidos:
     *
     * OWNER
     * ADMIN
     *
     * STAFF:
     *
     * 403 Forbidden
     *
     * PATCH:
     *
     * únicamente se modifican los campos no-null
     * incluidos en BusinessUpdateRequest.
     *
     * Ruta mantenida por compatibilidad.
     */
    @PatchMapping(
        "/{businessId}"
    )
    public ResponseEntity<BusinessResponse>
        update(

            @PathVariable
            Long businessId,

            @Valid
            @RequestBody
            BusinessUpdateRequest request
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        /*
         * OWNER / ADMIN.
         */
        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        BusinessResponse response =
            businessService.update(
                businessId,
                request
            );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // UPDATE BUSINESS PROFILE · CURRENT BUSINESS
    // =========================================================

    /**
     * Actualiza parcialmente el Business perteneciente
     * a la sesión actual.
     *
     * Endpoint recomendado para /business/settings.
     *
     * PATCH /api/v1/businesses/current
     *
     * Permitidos:
     *
     * OWNER
     * ADMIN
     *
     * STAFF:
     *
     * 403 Forbidden
     *
     * El businessId nunca llega desde el navegador.
     * Se deriva exclusivamente del principal autenticado.
     */
    @PatchMapping(
        "/current"
    )
    public ResponseEntity<BusinessResponse>
        updateCurrentBusiness(

            @Valid
            @RequestBody
            BusinessUpdateRequest request
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        Long businessId =
            requireCurrentBusinessId(
                authenticatedUser
            );

        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        BusinessResponse response =
            businessService.update(
                businessId,
                request
            );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // CURRENT BUSINESS ID
    // =========================================================

    /**
     * Recupera el businessId directamente del principal.
     *
     * Nunca confiamos en un businessId proporcionado
     * por el frontend para los endpoints "current".
     */
    private Long requireCurrentBusinessId(
        AuthenticatedUser authenticatedUser
    ) {

        if (
            authenticatedUser == null
                || authenticatedUser.getBusinessId() == null
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un contexto de negocio válido."
            );
        }

        Long businessId =
            authenticatedUser.getBusinessId();

        if (
            businessId <= 0
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada contiene "
                    + "un businessId no válido."
            );
        }

        return businessId;
    }

    // =========================================================
    // AUTHENTICATED USER
    // =========================================================

    /**
     * Recupera el principal generado por
     * JwtAuthenticationFilter.
     *
     * Aquí comprobamos la integridad estructural
     * de la sesión.
     *
     * La autorización tenant/rol persistida sigue
     * correspondiendo a BusinessMembershipService.
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
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un userId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() == null
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada no contiene "
                    + "un businessId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() <= 0
        ) {
            throw new BadCredentialsException(
                "La sesión autenticada contiene "
                    + "un businessId no válido."
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
}