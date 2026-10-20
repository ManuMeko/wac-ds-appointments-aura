package com.wacds.appointments.publicbooking;

import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignment;
import com.wacds.appointments.service.Service;
import com.wacds.appointments.support.IsolatedApiIntegrationSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de la reserva publica.
 */
class PublicBookingRulesIntegrationTests
    extends IsolatedApiIntegrationSupport {

    private OperationalBusiness tenant;

    @BeforeEach
    void createBookableBusiness() {

        tenant = createOperationalBusiness("public-booking");
    }

    @Test
    @DisplayName("Una reserva en un hueco libre queda confirmada")
    void booksAnOpenSlot() throws Exception {

        mockMvc.perform(
            post("/api/v1/public/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(tenant.startAt(), "ok"))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.appointmentId").isNumber())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.managementToken").isNotEmpty())
            .andExpect(
                jsonPath("$.businessId").value(tenant.businessId())
            );

        assertEquals(1L, countAppointments());
    }

    @Test
    @DisplayName("Un hueco ya ocupado responde conflicto")
    void occupiedSlotConflicts() throws Exception {

        mockMvc.perform(
            post("/api/v1/public/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(tenant.startAt(), "one"))
        ).andExpect(status().isCreated());

        mockMvc.perform(
            post("/api/v1/public/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(tenant.startAt(), "two"))
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertEquals(1L, countAppointments());
    }

    @Test
    @DisplayName(
        "Una fecha fuera del horizonte de settings es rechazada"
    )
    void bookingBeyondHorizonConflicts() throws Exception {

        updateHorizon(1);

        mockMvc.perform(
            post("/api/v1/public/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    bookingBody(
                        atLocal(
                            tenant.bookingDate().plusDays(10),
                            11,
                            0
                        ),
                        "horizon"
                    )
                )
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertEquals(0L, countAppointments());
    }

    @Test
    @DisplayName(
        "Una reserva dentro de la antelacion minima es rechazada"
    )
    void bookingInsideMinimumNoticeConflicts() throws Exception {

        updateMinimumNotice(10 * 24 * 60);

        mockMvc.perform(
            post("/api/v1/public/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(tenant.startAt(), "notice"))
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertEquals(0L, countAppointments());
    }

    @Test
    @DisplayName(
        "Un profesional que no ofrece el servicio no se puede reservar"
    )
    void professionalWithoutServiceConflicts() throws Exception {

        Long unassignedServiceId = persistService(true, false);

        mockMvc.perform(
            post("/api/v1/public/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    bookingJson(
                        tenant.businessId(),
                        tenant.professionalId(),
                        unassignedServiceId,
                        tenant.startAt(),
                        uniqueEmail("unassigned")
                    )
                )
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertEquals(0L, countAppointments());
    }

    @Test
    @DisplayName("Un servicio inactivo no se puede reservar")
    void inactiveServiceConflicts() throws Exception {

        Long inactiveServiceId = persistService(false, true);

        mockMvc.perform(
            post("/api/v1/public/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    bookingJson(
                        tenant.businessId(),
                        tenant.professionalId(),
                        inactiveServiceId,
                        tenant.startAt(),
                        uniqueEmail("inactive")
                    )
                )
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertEquals(0L, countAppointments());
    }

    private String bookingBody(
        java.time.OffsetDateTime startAt,
        String label
    ) {

        return bookingJson(
            tenant.businessId(),
            tenant.professionalId(),
            tenant.serviceId(),
            startAt,
            uniqueEmail(label)
        );
    }

    private String uniqueEmail(String label) {

        return label + "-" + newSuffix() + "@example.com";
    }

    private void updateHorizon(int days) {

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            settings().setBookingHorizonDays(days);

            entityManager.flush();
        });
    }

    private void updateMinimumNotice(int minutes) {

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            settings().setMinimumBookingNoticeMinutes(minutes);

            entityManager.flush();
        });
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

    private Long persistService(
        boolean active,
        boolean assignToProfessional
    ) {

        return transactionTemplate.execute(status -> {

            assertTransactionUsesTestDatabase();

            Business business = entityManager.find(
                Business.class,
                tenant.businessId()
            );

            Service service = Service.builder()
                .business(business)
                .name(active ? "Servicio extra" : "Servicio inactivo")
                .slug(
                    (active ? "extra-" : "inactivo-") + newSuffix()
                )
                .durationMinutes(30)
                .price(BigDecimal.ZERO)
                .active(active)
                .bookingEnabled(true)
                .build();

            entityManager.persist(service);
            entityManager.flush();

            if (assignToProfessional) {

                Professional professional = entityManager.find(
                    Professional.class,
                    tenant.professionalId()
                );

                entityManager.persist(
                    ProfessionalServiceAssignment.create(
                        professional,
                        service
                    )
                );

                entityManager.flush();
            }

            return service.getId();
        });
    }

    private long countAppointments() {

        Long count = transactionTemplate.execute(status -> {

            assertTransactionUsesTestDatabase();

            Number result = (Number) entityManager
                .createNativeQuery(
                    """
                    SELECT COUNT(*)
                    FROM appointments
                    WHERE business_id = :businessId
                    """
                )
                .setParameter("businessId", tenant.businessId())
                .getSingleResult();

            return result.longValue();
        });

        return count == null ? 0L : count;
    }
}
