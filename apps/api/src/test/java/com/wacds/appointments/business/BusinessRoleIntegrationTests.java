package com.wacds.appointments.business;

import com.wacds.appointments.auth.membership.BusinessRole;
import com.wacds.appointments.support.IsolatedApiIntegrationSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * STAFF no entra en la gestion reservada a OWNER y ADMIN.
 */
class BusinessRoleIntegrationTests
    extends IsolatedApiIntegrationSupport {

    private OperationalBusiness tenant;

    private Session staffSession;

    private Session ownerSession;

    @BeforeEach
    void createOwnerAndStaff() throws Exception {

        tenant = createOperationalBusiness("roles");

        MemberCredentials staff = addMember(
            tenant.businessId(),
            BusinessRole.STAFF,
            "staff"
        );

        staffSession = login(staff.email(), TEST_PASSWORD);

        ownerSession = login(tenant.email(), TEST_PASSWORD);
    }

    @Test
    @DisplayName(
        "STAFF no puede leer ni editar los settings del negocio"
    )
    void staffCannotManageSettings() throws Exception {

        mockMvc.perform(
            get(
                "/api/v1/businesses/{businessId}/settings",
                tenant.businessId()
            ).header(
                "Authorization",
                bearer(staffSession.accessToken())
            )
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            patch(
                "/api/v1/businesses/{businessId}/settings",
                tenant.businessId()
            )
                .header(
                    "Authorization",
                    bearer(staffSession.accessToken())
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotIntervalMinutes\":20}")
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            get(
                "/api/v1/businesses/{businessId}/settings",
                tenant.businessId()
            ).header(
                "Authorization",
                bearer(ownerSession.accessToken())
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slotIntervalMinutes").value(15));
    }

    @Test
    @DisplayName("STAFF no puede editar el negocio")
    void staffCannotUpdateBusiness() throws Exception {

        mockMvc.perform(
            patch(
                "/api/v1/businesses/{businessId}",
                tenant.businessId()
            )
                .header(
                    "Authorization",
                    bearer(staffSession.accessToken())
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cambiado por staff\"}")
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            patch(
                "/api/v1/businesses/{businessId}",
                tenant.businessId()
            )
                .header(
                    "Authorization",
                    bearer(ownerSession.accessToken())
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cambiado por owner\"}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Cambiado por owner"));
    }
}
