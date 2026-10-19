package com.wacds.appointments.dashboard;

import com.wacds.appointments.auth.membership.BusinessMembershipService;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.dashboard.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/v1/businesses/{businessId}/dashboard"
)
@RequiredArgsConstructor
public class DashboardController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final DashboardService dashboardService;

    private final BusinessMembershipService
        businessMembershipService;

    // =========================================================
    // BUSINESS DASHBOARD
    // =========================================================

    @GetMapping
    public ResponseEntity<DashboardSummaryResponse>
        getBusinessDashboard(
            @PathVariable
            Long businessId,

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
        ) {

        /*
         * Seguridad multiempresa:
         *
         * - JWT válido
         * - usuario autenticado
         * - businessId del JWT coincide con el solicitado
         * - membership activa
         * - business activo
         * - role actual coincide con el role de la sesión
         *
         * OWNER, ADMIN y STAFF pueden consultar el dashboard.
         */
        businessMembershipService
            .requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        DashboardSummaryResponse response =
            dashboardService
                .getBusinessDashboard(
                    businessId
                );

        return ResponseEntity.ok(
            response
        );
    }
}