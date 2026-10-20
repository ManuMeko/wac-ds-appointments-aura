package com.wacds.appointments.availabilityslot;

import com.wacds.appointments.support.IsolatedApiIntegrationSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La disponibilidad publica oculta huecos ocupados
 * y dias cerrados por excepcion.
 */
class AvailabilitySlotIntegrationTests
    extends IsolatedApiIntegrationSupport {

    private OperationalBusiness tenant;

    private LocalDate closedDate;

    @BeforeEach
    void createAgenda() {

        tenant = createOperationalBusiness("availability");

        persistConfirmedAppointment(
            tenant.businessId(),
            tenant.customerId(),
            tenant.professionalId(),
            tenant.serviceId(),
            tenant.startAt()
        );

        closedDate = tenant.bookingDate().plusDays(1);

        persistUnavailableDay(
            tenant.professionalId(),
            closedDate
        );
    }

    @Test
    @DisplayName(
        "No devuelve huecos solapados con una cita existente"
    )
    void hidesSlotsOverlappingAnAppointment() throws Exception {

        mockMvc.perform(
            get("/api/v1/public/availability-slots")
                .param(
                    "professionalId",
                    tenant.professionalId().toString()
                )
                .param(
                    "serviceId",
                    tenant.serviceId().toString()
                )
                .param("date", tenant.bookingDate().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath(slot("11:00")).isEmpty())
            .andExpect(jsonPath(slot("10:45")).isEmpty())
            .andExpect(jsonPath(slot("09:00")).isNotEmpty())
            .andExpect(jsonPath(slot("11:30")).isNotEmpty());
    }

    @Test
    @DisplayName(
        "No devuelve huecos en un dia cerrado por excepcion"
    )
    void hidesSlotsOnUnavailableException() throws Exception {

        mockMvc.perform(
            get("/api/v1/public/availability-slots")
                .param(
                    "professionalId",
                    tenant.professionalId().toString()
                )
                .param(
                    "serviceId",
                    tenant.serviceId().toString()
                )
                .param("date", closedDate.toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSlots").value(0))
            .andExpect(jsonPath("$.slots").isEmpty());
    }

    private static String slot(String time) {

        return "$.slots[?(@.startTime == '"
            + time
            + "' || @.startTime == '"
            + time
            + ":00')]";
    }
}
