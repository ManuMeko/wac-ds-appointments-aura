package com.wacds.appointments.publicbooking.management;

import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.publicbooking.management.PublicBookingManagementController;
import com.wacds.appointments.support.IsolatedApiIntegrationSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestion publica de una reserva mediante su token.
 */
class PublicBookingManagementIntegrationTests
    extends IsolatedApiIntegrationSupport {

    private static final String TOKEN_HEADER =
        PublicBookingManagementController.MANAGEMENT_TOKEN_HEADER;

    private OperationalBusiness tenant;

    @BeforeEach
    void createBookableBusiness() {

        tenant = createOperationalBusiness("management");
    }

    @Test
    @DisplayName("Un token valido reprograma la reserva")
    void validTokenReschedules() throws Exception {

        String token = book(tenant.startAt(), "reschedule");

        OffsetDateTime newStart = atLocal(
            tenant.bookingDate(),
            16,
            0
        );

        mockMvc.perform(
            post("/api/v1/public/bookings/manage/reschedule")
                .header(TOKEN_HEADER, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"startAt":"%s"}
                    """.formatted(
                        newStart.format(
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        )
                    )
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(
                jsonPath("$.startAt")
                    .value(
                        newStart.format(
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        )
                    )
            );
    }

    @Test
    @DisplayName("Un token valido cancela la reserva")
    void validTokenCancels() throws Exception {

        String token = book(tenant.startAt(), "cancel");

        mockMvc.perform(
            post("/api/v1/public/bookings/manage/cancel")
                .header(TOKEN_HEADER, token)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Un token invalido es rechazado")
    void invalidTokenIsRejected() throws Exception {

        mockMvc.perform(
            get("/api/v1/public/bookings/manage")
                .header(
                    TOKEN_HEADER,
                    "invalid-management-token-not-real-0001"
                )
        )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code").value("RESOURCE_NOT_FOUND")
            );
    }

    @Test
    @DisplayName(
        "No cancela si la cancelacion online esta desactivada"
    )
    void cancellationFlagBlocksCancel() throws Exception {

        String token = book(tenant.startAt(), "no-cancel");

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            settings().disableOnlineCancellation();

            entityManager.flush();
        });

        mockMvc.perform(
            post("/api/v1/public/bookings/manage/cancel")
                .header(TOKEN_HEADER, token)
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(
            get("/api/v1/public/bookings/manage")
                .header(TOKEN_HEADER, token)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName(
        "No reprograma si la reprogramacion online esta desactivada"
    )
    void reschedulingFlagBlocksReschedule() throws Exception {

        String token = book(tenant.startAt(), "no-reschedule");

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            settings().disableOnlineRescheduling();

            entityManager.flush();
        });

        OffsetDateTime newStart = atLocal(
            tenant.bookingDate(),
            16,
            0
        );

        mockMvc.perform(
            post("/api/v1/public/bookings/manage/reschedule")
                .header(TOKEN_HEADER, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"startAt":"%s"}
                    """.formatted(
                        newStart.format(
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        )
                    )
                )
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(
            get("/api/v1/public/bookings/manage")
                .header(TOKEN_HEADER, token)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    private String book(
        OffsetDateTime startAt,
        String label
    ) throws Exception {

        MvcResult result = mockMvc.perform(
            post("/api/v1/public/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    bookingJson(
                        tenant.businessId(),
                        tenant.professionalId(),
                        tenant.serviceId(),
                        startAt,
                        label + "-" + newSuffix() + "@example.com"
                    )
                )
        )
            .andExpect(status().isCreated())
            .andReturn();

        return extractJsonString(
            result.getResponse().getContentAsString(),
            "managementToken"
        );
    }

    private BusinessSettings settings() {

        return entityManager
            .createQuery(
                """
                select settings
                from BusinessSettings settings
                where settings.business.id = :businessId
                """,
                BusinessSettings.class
            )
            .setParameter("businessId", tenant.businessId())
            .getSingleResult();
    }
}
