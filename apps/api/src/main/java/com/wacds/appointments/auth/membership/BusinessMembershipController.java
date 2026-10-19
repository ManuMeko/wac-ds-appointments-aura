package com.wacds.appointments.auth.membership;

import com.wacds.appointments.auth.membership.dto.BusinessMembershipCreateRequest;
import com.wacds.appointments.auth.membership.dto.BusinessMembershipResponse;
import com.wacds.appointments.auth.membership.dto.BusinessMembershipRoleUpdateRequest;
import com.wacds.appointments.auth.membership.dto.BusinessMembershipStatusUpdateRequest;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
    "/api/v1/businesses/{businessId}/members"
)
@RequiredArgsConstructor
public class BusinessMembershipController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessMembershipService
        businessMembershipService;

    private final BusinessMembershipManagementService
        businessMembershipManagementService;

    private final BusinessMembershipMapper
        businessMembershipMapper;

    // =========================================================
    // CREATE MEMBER
    // =========================================================

    /**
     * Añade un miembro al business.
     *
     * Soporta:
     *
     * - usuario nuevo
     * - usuario existente
     * - reactivación de membership inactiva
     *
     * Reglas:
     *
     * OWNER:
     *
     * - ADMIN
     * - STAFF
     *
     * ADMIN:
     *
     * - STAFF
     *
     * STAFF:
     *
     * - sin acceso
     */
    @PostMapping
    public ResponseEntity<BusinessMembershipResponse>
        createMember(
            @PathVariable
            Long businessId,

            @Valid
            @RequestBody
            BusinessMembershipCreateRequest request
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        BusinessMembership membership =
            businessMembershipManagementService
                .createMember(
                    authenticatedUser,
                    businessId,
                    request
                );

        BusinessMembershipResponse response =
            businessMembershipMapper
                .toResponse(
                    membership
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
    // LIST MEMBERS
    // =========================================================

    /**
     * Devuelve el equipo completo del business.
     *
     * Incluye:
     *
     * - memberships activas
     * - memberships inactivas
     *
     * Esto permite que el panel pueda mostrar miembros
     * desactivados y ofrecer su reactivación.
     *
     * Permitidos:
     *
     * OWNER
     * ADMIN
     *
     * STAFF:
     *
     * 403 Forbidden
     */
    @GetMapping
    public ResponseEntity<
        List<BusinessMembershipResponse>
    > findMembers(
        @PathVariable
        Long businessId
    ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        List<BusinessMembership> memberships =
            businessMembershipService
                .findMembershipsForBusiness(
                    authenticatedUser,
                    businessId
                );

        List<BusinessMembershipResponse> response =
            businessMembershipMapper
                .toResponseList(
                    memberships
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // FIND MEMBER
    // =========================================================

    /**
     * Devuelve una membership concreta.
     *
     * Puede estar:
     *
     * - activa
     * - inactiva
     *
     * Permitidos:
     *
     * OWNER
     * ADMIN
     */
    @GetMapping(
        "/{membershipId}"
    )
    public ResponseEntity<BusinessMembershipResponse>
        findMember(
            @PathVariable
            Long businessId,

            @PathVariable
            Long membershipId
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        BusinessMembership membership =
            businessMembershipService
                .findMembershipForManagement(
                    authenticatedUser,
                    businessId,
                    membershipId
                );

        BusinessMembershipResponse response =
            businessMembershipMapper
                .toResponse(
                    membership
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // UPDATE ROLE
    // =========================================================

    /**
     * Cambia el rol de un miembro.
     *
     * OWNER:
     *
     * - ADMIN -> STAFF
     * - STAFF -> ADMIN
     *
     * ADMIN:
     *
     * - solo gestiona STAFF
     *
     * No se permite:
     *
     * - asignar OWNER
     * - modificar OWNER
     * - modificar la propia membership
     */
    @PatchMapping(
        "/{membershipId}/role"
    )
    public ResponseEntity<BusinessMembershipResponse>
        updateRole(
            @PathVariable
            Long businessId,

            @PathVariable
            Long membershipId,

            @Valid
            @RequestBody
            BusinessMembershipRoleUpdateRequest request
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        BusinessMembership membership =
            businessMembershipService
                .updateMemberRole(
                    authenticatedUser,
                    businessId,
                    membershipId,
                    request.role()
                );

        BusinessMembershipResponse response =
            businessMembershipMapper
                .toResponse(
                    membership
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    /**
     * Activa o desactiva una membership.
     *
     * IMPORTANTE:
     *
     * Esta operación modifica únicamente:
     *
     * BusinessMembership.active
     *
     * NO modifica:
     *
     * User.active
     *
     * Por tanto, una persona puede perder acceso a un business
     * sin desactivar su cuenta global.
     *
     * OWNER:
     *
     * - gestiona ADMIN
     * - gestiona STAFF
     *
     * ADMIN:
     *
     * - gestiona STAFF
     *
     * No se permite:
     *
     * - modificar OWNER
     * - modificar la propia membership
     */
    @PatchMapping(
        "/{membershipId}/status"
    )
    public ResponseEntity<BusinessMembershipResponse>
        updateStatus(
            @PathVariable
            Long businessId,

            @PathVariable
            Long membershipId,

            @Valid
            @RequestBody
            BusinessMembershipStatusUpdateRequest request
        ) {

        AuthenticatedUser authenticatedUser =
            requireAuthenticatedUser();

        BusinessMembership membership =
            businessMembershipService
                .updateMemberActiveStatus(
                    authenticatedUser,
                    businessId,
                    membershipId,
                    request.active()
                );

        BusinessMembershipResponse response =
            businessMembershipMapper
                .toResponse(
                    membership
                );

        return ResponseEntity.ok(
            response
        );
    }

    // =========================================================
    // AUTHENTICATED USER
    // =========================================================

    /**
     * Recupera el principal generado por JwtAuthenticationFilter.
     *
     * Aquí solo validamos que exista una sesión estructuralmente
     * válida.
     *
     * La autorización real por tenant y rol pertenece a:
     *
     * - BusinessMembershipService
     * - BusinessMembershipManagementService
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