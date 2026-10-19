package com.wacds.appointments.business.settings;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.business.dto.BusinessSettingsResponse;
import com.wacds.appointments.business.dto.BusinessSettingsUpdateRequest;
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
    "/api/v1/businesses/{businessId}/settings"
)
@RequiredArgsConstructor
public class BusinessSettingsController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessSettingsService
        businessSettingsService;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // GET SETTINGS
    // =========================================================

    /**
     * Devuelve la configuración operativa del business.
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
     * La autorización real se valida contra la membership
     * persistida en PostgreSQL y no únicamente contra los
     * claims incluidos en el JWT.
     */
    @GetMapping
    public ResponseEntity<BusinessSettingsResponse>
        findSettings(
            @PathVariable
            Long businessId
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        /*
         * Business settings pertenece a configuración
         * administrativa del tenant.
         *
         * OWNER / ADMIN únicamente.
         */
        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        BusinessSettingsResponse response =
            businessSettingsService
                .findByBusinessId(
                    businessId
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // PATCH SETTINGS
    // =========================================================

    /**
     * Actualiza parcialmente la configuración operativa.
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
     * Semántica PATCH:
     *
     * - null = no modificar
     * - valor = modificar
     *
     * BusinessSettingsService también controla:
     *
     * - invariantes de dominio;
     * - políticas de recordatorios;
     * - límites de reserva;
     * - optimistic locking;
     * - versionado concurrente.
     */
    @PatchMapping
    public ResponseEntity<BusinessSettingsResponse>
        updateSettings(
            @PathVariable
            Long businessId,

            @Valid
            @RequestBody
            BusinessSettingsUpdateRequest request
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        /*
         * OWNER / ADMIN únicamente.
         */
        businessMembershipService
            .requireBusinessManagement(
                authenticatedUser,
                businessId
            );

        BusinessSettingsResponse response =
            businessSettingsService.update(
                businessId,
                request
            );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // AUTHENTICATED USER
    // =========================================================

    /**
     * Recupera el principal creado por JwtAuthenticationFilter.
     *
     * Este método valida únicamente que la autenticación
     * contenga la estructura mínima necesaria.
     *
     * La autorización tenant/role se realiza posteriormente
     * mediante BusinessMembershipService.
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