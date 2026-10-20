package com.wacds.appointments.auth;

import com.wacds.appointments.support.IsolatedApiIntegrationSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login, refresh, logout y acceso sin token.
 */
class AuthIntegrationTests extends IsolatedApiIntegrationSupport {

    private OperationalBusiness tenant;

    @BeforeEach
    void createUser() {

        tenant = createOperationalBusiness("auth");
    }

    @Test
    @DisplayName("El login correcto devuelve sesion y abre /auth/me")
    void loginSucceeds() throws Exception {

        Session session = login(
            tenant.email(),
            TEST_PASSWORD
        );

        mockMvc.perform(
            get("/api/v1/auth/me")
                .header(
                    "Authorization",
                    bearer(session.accessToken())
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(tenant.email()))
            .andExpect(jsonPath("$.businessId").value(tenant.businessId()))
            .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    @DisplayName("Una contrasena incorrecta responde 401")
    void wrongPasswordIsRejected() throws Exception {

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"incorrecta-1"}
                    """.formatted(tenant.email())
                )
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Un refresh valido renueva el access token")
    void validRefreshRotatesSession() throws Exception {

        Session session = login(
            tenant.email(),
            TEST_PASSWORD
        );

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"%s"}
                    """.formatted(session.refreshToken())
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(
                jsonPath("$.refreshToken")
                    .value(
                        org.hamcrest.Matchers.not(
                            session.refreshToken()
                        )
                    )
            );
    }

    @Test
    @DisplayName("Tras el logout el refresh queda revocado")
    void refreshIsRejectedAfterLogout() throws Exception {

        Session session = login(
            tenant.email(),
            TEST_PASSWORD
        );

        mockMvc.perform(
            post("/api/v1/auth/logout")
                .header(
                    "Authorization",
                    bearer(session.accessToken())
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"%s"}
                    """.formatted(session.refreshToken())
                )
        )
            .andExpect(status().isNoContent());

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"%s"}
                    """.formatted(session.refreshToken())
                )
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un endpoint protegido sin token responde 401")
    void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {

        mockMvc.perform(
            get("/api/v1/auth/me")
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
