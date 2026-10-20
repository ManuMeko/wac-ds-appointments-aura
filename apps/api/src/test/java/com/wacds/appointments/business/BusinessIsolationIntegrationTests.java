package com.wacds.appointments.business;

import com.wacds.appointments.support.IsolatedApiIntegrationSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un usuario no puede leer ni modificar datos de otro negocio.
 */
class BusinessIsolationIntegrationTests
    extends IsolatedApiIntegrationSupport {

    private OperationalBusiness ownerTenant;

    private OperationalBusiness foreignTenant;

    private Long foreignAppointmentId;

    private Session ownerSession;

    @BeforeEach
    void createTwoBusinesses() throws Exception {

        ownerTenant = createOperationalBusiness("isolation-a");

        foreignTenant = createOperationalBusiness("isolation-b");

        foreignAppointmentId = persistConfirmedAppointment(
            foreignTenant.businessId(),
            foreignTenant.customerId(),
            foreignTenant.professionalId(),
            foreignTenant.serviceId(),
            foreignTenant.startAt()
        );

        ownerSession = login(
            ownerTenant.email(),
            TEST_PASSWORD
        );
    }

    @Test
    @DisplayName(
        "El dueno lee la cita de su negocio"
    )
    void ownerReadsOwnAppointment() throws Exception {

        Long ownAppointmentId = persistConfirmedAppointment(
            ownerTenant.businessId(),
            ownerTenant.customerId(),
            ownerTenant.professionalId(),
            ownerTenant.serviceId(),
            ownerTenant.startAt()
        );

        mockMvc.perform(
            get(
                "/api/v1/appointments/{appointmentId}",
                ownAppointmentId
            )
                .header(
                    "Authorization",
                    bearer(ownerSession.accessToken())
                )
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.businessId")
                    .value(ownerTenant.businessId())
            );
    }

    @Test
    @DisplayName(
        "No puede leer citas, clientes, servicios "
            + "ni profesionales de otro negocio"
    )
    void cannotReadAnotherBusiness() throws Exception {

        String authorization = bearer(ownerSession.accessToken());

        mockMvc.perform(
            get(
                "/api/v1/businesses/{businessId}/appointments/{appointmentId}",
                foreignTenant.businessId(),
                foreignAppointmentId
            ).header("Authorization", authorization)
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            get(
                "/api/v1/appointments/{appointmentId}",
                foreignAppointmentId
            ).header("Authorization", authorization)
        ).andExpect(status().isNotFound());

        mockMvc.perform(
            get(
                "/api/v1/businesses/{businessId}/customers/{customerId}",
                foreignTenant.businessId(),
                foreignTenant.customerId()
            ).header("Authorization", authorization)
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            get(
                "/api/v1/customers/{customerId}",
                foreignTenant.customerId()
            ).header("Authorization", authorization)
        ).andExpect(status().isNotFound());

        mockMvc.perform(
            get(
                "/api/v1/businesses/{businessId}/services/{serviceId}",
                foreignTenant.businessId(),
                foreignTenant.serviceId()
            ).header("Authorization", authorization)
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            get(
                "/api/v1/services/{serviceId}",
                foreignTenant.serviceId()
            ).header("Authorization", authorization)
        ).andExpect(status().isNotFound());

        mockMvc.perform(
            get(
                "/api/v1/businesses/{businessId}/professionals/{professionalId}",
                foreignTenant.businessId(),
                foreignTenant.professionalId()
            ).header("Authorization", authorization)
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            get(
                "/api/v1/professionals/{professionalId}",
                foreignTenant.professionalId()
            ).header("Authorization", authorization)
        ).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName(
        "No puede modificar citas, clientes, servicios "
            + "ni profesionales de otro negocio"
    )
    void cannotModifyAnotherBusiness() throws Exception {

        String authorization = bearer(ownerSession.accessToken());

        mockMvc.perform(
            patch(
                "/api/v1/appointments/{appointmentId}/status",
                foreignAppointmentId
            )
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}")
        ).andExpect(status().isNotFound());

        mockMvc.perform(
            put(
                "/api/v1/businesses/{businessId}/customers/{customerId}",
                foreignTenant.businessId(),
                foreignTenant.customerId()
            )
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerBody("Intruso"))
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            put(
                "/api/v1/customers/{customerId}",
                foreignTenant.customerId()
            )
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerBody("Intruso"))
        ).andExpect(status().isNotFound());

        mockMvc.perform(
            put(
                "/api/v1/businesses/{businessId}/services/{serviceId}",
                foreignTenant.businessId(),
                foreignTenant.serviceId()
            )
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceBody())
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            put(
                "/api/v1/services/{serviceId}",
                foreignTenant.serviceId()
            )
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serviceBody())
        ).andExpect(status().isNotFound());

        mockMvc.perform(
            put(
                "/api/v1/businesses/{businessId}/professionals/{professionalId}",
                foreignTenant.businessId(),
                foreignTenant.professionalId()
            )
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(professionalBody())
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            put(
                "/api/v1/professionals/{professionalId}",
                foreignTenant.professionalId()
            )
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(professionalBody())
        ).andExpect(status().isNotFound());

        Session foreignSession = login(
            foreignTenant.email(),
            TEST_PASSWORD
        );

        mockMvc.perform(
            get(
                "/api/v1/appointments/{appointmentId}",
                foreignAppointmentId
            ).header(
                "Authorization",
                bearer(foreignSession.accessToken())
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(
            get(
                "/api/v1/customers/{customerId}",
                foreignTenant.customerId()
            ).header(
                "Authorization",
                bearer(foreignSession.accessToken())
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Fixture"));

        mockMvc.perform(
            get(
                "/api/v1/services/{serviceId}",
                foreignTenant.serviceId()
            ).header(
                "Authorization",
                bearer(foreignSession.accessToken())
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Servicio operativo"));

        mockMvc.perform(
            get(
                "/api/v1/professionals/{professionalId}",
                foreignTenant.professionalId()
            ).header(
                "Authorization",
                bearer(foreignSession.accessToken())
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Operativo"));
    }

    private String customerBody(String firstName) {

        return """
            {
              "firstName": "%s",
              "lastName": "Customer",
              "email": "intruso@example.com",
              "phone": "+34600000000",
              "notes": null,
              "active": true
            }
            """.formatted(firstName);
    }

    private String serviceBody() {

        return """
            {
              "name": "Servicio ajeno",
              "slug": "servicio-ajeno",
              "description": null,
              "durationMinutes": 30,
              "price": 0,
              "active": true,
              "bookingEnabled": true
            }
            """;
    }

    private String professionalBody() {

        return """
            {
              "firstName": "Ajeno",
              "lastName": "Professional",
              "displayName": "Ajeno Professional",
              "email": null,
              "phone": null,
              "bio": null,
              "active": true,
              "bookingEnabled": true
            }
            """;
    }
}
