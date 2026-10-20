package com.wacds.appointments.support;

import com.wacds.appointments.appointment.Appointment;
import com.wacds.appointments.auth.membership.BusinessMembership;
import com.wacds.appointments.auth.membership.BusinessRole;
import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.availability.ProfessionalAvailability;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.customer.Customer;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignment;
import com.wacds.appointments.reminder.ReminderDeliveryService.DeliveryResult;
import com.wacds.appointments.reminder.ResendEmailReminderDeliveryService;
import com.wacds.appointments.scheduleexception.ScheduleException;
import com.wacds.appointments.service.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base de las pruebas HTTP de integración.
 *
 * Usa el mismo perfil, la misma base PostgreSQL aislada
 * y el mismo mock de Resend que el resto de la suite.
 * Cada prueba borra solo los negocios y usuarios que crea.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.bootstrap.dev-owner.enabled=false",
        "app.public-booking.confirmation-email.enabled=false",
        "app.reminders.email.real-delivery-enabled=false",
        "app.reminders.scheduler.enabled=false"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IsolatedApiIntegrationSupport {

    protected static final String EXPECTED_DATABASE =
        "wac_ds_appointments_test";

    protected static final String BUSINESS_TIMEZONE =
        "Europe/Madrid";

    protected static final String TEST_PASSWORD =
        "WacdsTest1";

    protected static final ZoneId BUSINESS_ZONE =
        ZoneId.of(BUSINESS_TIMEZONE);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @MockitoBean
    protected ResendEmailReminderDeliveryService emailDelivery;

    @PersistenceContext
    protected EntityManager entityManager;

    protected TransactionTemplate transactionTemplate;

    private final List<Long> businessIds = new ArrayList<>();

    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void prepareIntegrationInfrastructure() throws Exception {

        assertIsolatedDatabase();

        when(
            emailDelivery.sendTransactionalEmail(
                anyString(),
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(
            DeliveryResult.sent(
                "integration-test"
            )
        );

        clearInvocations(emailDelivery);

        transactionTemplate =
            new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void deleteTrackedDataAndAssertNoEmail() throws Exception {

        try {

            assertIsolatedDatabase();

            if (transactionTemplate == null) {
                return;
            }

            for (Long businessId : businessIds) {
                deleteBusiness(businessId);
            }

            for (Long userId : userIds) {
                deleteUser(userId);
            }

        } finally {

            businessIds.clear();
            userIds.clear();

            verify(emailDelivery, never())
                .sendTransactionalEmail(
                    anyString(),
                    anyString(),
                    anyString(),
                    anyString()
                );
        }
    }

    protected void trackBusiness(Long businessId) {

        if (
            businessId != null
                && !businessIds.contains(businessId)
        ) {
            businessIds.add(businessId);
        }
    }

    protected void trackUser(Long userId) {

        if (
            userId != null
                && !userIds.contains(userId)
        ) {
            userIds.add(userId);
        }
    }

    protected String newSuffix() {

        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 16);
    }

    protected OffsetDateTime atLocal(
        LocalDate date,
        int hour,
        int minute
    ) {

        return date
            .atTime(hour, minute)
            .atZone(BUSINESS_ZONE)
            .toOffsetDateTime();
    }

    protected OperationalBusiness createOperationalBusiness(
        String namePrefix
    ) {

        return createOperationalBusiness(
            namePrefix,
            0,
            60,
            true,
            true
        );
    }

    protected OperationalBusiness createOperationalBusiness(
        String namePrefix,
        int minimumBookingNoticeMinutes,
        int bookingHorizonDays,
        boolean onlineCancellationEnabled,
        boolean onlineReschedulingEnabled
    ) {

        String suffix = newSuffix();

        LocalDate bookingDate =
            LocalDate.now(BUSINESS_ZONE).plusDays(2);

        OffsetDateTime startAt =
            atLocal(bookingDate, 11, 0);

        String email =
            namePrefix + "-" + suffix + "@example.com";

        OperationalBusiness created =
            transactionTemplate.execute(status -> {

                assertTransactionUsesTestDatabase();

                Business business = Business.builder()
                    .name("WAC DS " + namePrefix)
                    .slug(namePrefix + "-" + suffix)
                    .email(
                        "business-" + suffix + "@example.com"
                    )
                    .timezone(BUSINESS_TIMEZONE)
                    .active(true)
                    .publicBookingEnabled(true)
                    .build();

                entityManager.persist(business);
                entityManager.flush();

                BusinessSettings settings =
                    BusinessSettings.builder()
                        .business(business)
                        .minimumBookingNoticeMinutes(
                            minimumBookingNoticeMinutes
                        )
                        .bookingHorizonDays(bookingHorizonDays)
                        .slotIntervalMinutes(15)
                        .defaultBufferBeforeMinutes(0)
                        .defaultBufferAfterMinutes(0)
                        .autoConfirmAppointments(true)
                        .onlineCancellationEnabled(
                            onlineCancellationEnabled
                        )
                        .minimumCancellationNoticeMinutes(0)
                        .onlineReschedulingEnabled(
                            onlineReschedulingEnabled
                        )
                        .minimumRescheduleNoticeMinutes(0)
                        .remindersEnabled(false)
                        .secondReminderEnabled(false)
                        .build();

                entityManager.persist(settings);
                entityManager.flush();

                User owner = User.builder()
                    .email(email)
                    .passwordHash(
                        passwordEncoder.encode(TEST_PASSWORD)
                    )
                    .firstName("Owner")
                    .lastName(namePrefix)
                    .active(true)
                    .build();

                entityManager.persist(owner);
                entityManager.flush();

                entityManager.persist(
                    BusinessMembership.builder()
                        .business(business)
                        .user(owner)
                        .role(BusinessRole.OWNER)
                        .active(true)
                        .build()
                );

                Customer customer = Customer.builder()
                    .business(business)
                    .firstName("Fixture")
                    .lastName("Customer")
                    .email(
                        "customer-" + suffix + "@example.com"
                    )
                    .phone("+34600000999")
                    .active(true)
                    .build();

                entityManager.persist(customer);
                entityManager.flush();

                Professional professional =
                    Professional.builder()
                        .business(business)
                        .firstName("Operativo")
                        .lastName("Professional")
                        .displayName("Operativo Professional")
                        .active(true)
                        .bookingEnabled(true)
                        .build();

                entityManager.persist(professional);
                entityManager.flush();

                Service service = Service.builder()
                    .business(business)
                    .name("Servicio operativo")
                    .slug("servicio-operativo-" + suffix)
                    .durationMinutes(30)
                    .price(BigDecimal.ZERO)
                    .active(true)
                    .bookingEnabled(true)
                    .build();

                entityManager.persist(service);
                entityManager.flush();

                entityManager.persist(
                    ProfessionalServiceAssignment.create(
                        professional,
                        service
                    )
                );

                for (DayOfWeek day : DayOfWeek.values()) {

                    entityManager.persist(
                        ProfessionalAvailability.createActive(
                            professional,
                            day,
                            LocalTime.of(9, 0),
                            LocalTime.of(18, 0)
                        )
                    );
                }

                entityManager.flush();

                return new OperationalBusiness(
                    business.getId(),
                    owner.getId(),
                    email,
                    professional.getId(),
                    service.getId(),
                    customer.getId(),
                    bookingDate,
                    startAt
                );
            });

        trackBusiness(created.businessId());
        trackUser(created.userId());

        return created;
    }

    protected MemberCredentials addMember(
        Long businessId,
        BusinessRole role,
        String namePrefix
    ) {

        String email =
            namePrefix + "-" + newSuffix() + "@example.com";

        Long userId = transactionTemplate.execute(status -> {

            assertTransactionUsesTestDatabase();

            Business business = entityManager.find(
                Business.class,
                businessId
            );

            User user = User.builder()
                .email(email)
                .passwordHash(
                    passwordEncoder.encode(TEST_PASSWORD)
                )
                .firstName(namePrefix)
                .lastName("Member")
                .active(true)
                .build();

            entityManager.persist(user);
            entityManager.flush();

            entityManager.persist(
                BusinessMembership.builder()
                    .business(business)
                    .user(user)
                    .role(role)
                    .active(true)
                    .build()
            );

            entityManager.flush();

            return user.getId();
        });

        trackUser(userId);

        return new MemberCredentials(userId, email);
    }

    protected Long persistConfirmedAppointment(
        Long businessId,
        Long customerId,
        Long professionalId,
        Long serviceId,
        OffsetDateTime startAt
    ) {

        return transactionTemplate.execute(status -> {

            assertTransactionUsesTestDatabase();

            Appointment appointment = Appointment.builder()
                .business(
                    entityManager.find(
                        Business.class,
                        businessId
                    )
                )
                .customer(
                    entityManager.find(
                        Customer.class,
                        customerId
                    )
                )
                .professional(
                    entityManager.find(
                        Professional.class,
                        professionalId
                    )
                )
                .service(
                    entityManager.find(
                        Service.class,
                        serviceId
                    )
                )
                .startAt(startAt)
                .endAt(startAt.plusMinutes(30))
                .status(Appointment.AppointmentStatus.CONFIRMED)
                .build();

            entityManager.persist(appointment);
            entityManager.flush();

            return appointment.getId();
        });
    }

    protected void persistUnavailableDay(
        Long professionalId,
        LocalDate date
    ) {

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            Professional professional =
                entityManager.find(
                    Professional.class,
                    professionalId
                );

            ScheduleException exception =
                ScheduleException.builder()
                    .professional(professional)
                    .exceptionDate(date)
                    .type(
                        ScheduleException.ExceptionType.UNAVAILABLE
                    )
                    .reason("Dia cerrado de prueba")
                    .active(true)
                    .build();

            entityManager.persist(exception);
            entityManager.flush();
        });
    }

    protected Session login(
        String email,
        String password
    ) throws Exception {

        MvcResult result = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"%s"}
                    """.formatted(email, password)
                )
        ).andReturn();

        String body = result.getResponse().getContentAsString();

        if (result.getResponse().getStatus() != 200) {
            throw new AssertionError(
                "El login de prueba no devolvio 200: " + body
            );
        }

        return new Session(
            extractJsonString(body, "accessToken"),
            extractJsonString(body, "refreshToken")
        );
    }

    protected String bookingJson(
        Long businessId,
        Long professionalId,
        Long serviceId,
        OffsetDateTime startAt,
        String email
    ) {

        return """
            {
              "businessId": %d,
              "professionalId": %d,
              "serviceId": %d,
              "startAt": "%s",
              "firstName": "Cliente",
              "lastName": "Publico",
              "email": "%s",
              "phone": "+34600111222",
              "notes": "integration-test"
            }
            """.formatted(
            businessId,
            professionalId,
            serviceId,
            startAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            email
        );
    }

    protected String bearer(String accessToken) {

        return "Bearer " + accessToken;
    }

    protected void assertIsolatedDatabase() throws Exception {

        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT current_database()"
            )
        ) {

            if (!resultSet.next()) {
                throw new AssertionError(
                    "PostgreSQL no devolvio current_database()."
                );
            }

            String actualDatabase = resultSet.getString(1);

            if (!EXPECTED_DATABASE.equals(actualDatabase)) {
                throw new AssertionError(
                    "OPERACION BLOQUEADA: las pruebas estan "
                        + "conectadas a " + actualDatabase + "."
                );
            }
        }
    }

    protected void assertTransactionUsesTestDatabase() {

        Object result = entityManager
            .createNativeQuery("SELECT current_database()")
            .getSingleResult();

        if (!EXPECTED_DATABASE.equals(String.valueOf(result))) {
            throw new AssertionError(
                "OPERACION BLOQUEADA: la transaccion no utiliza "
                    + "la base TEST."
            );
        }
    }

    private void deleteBusiness(Long businessId) {

        if (businessId == null) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            executeDelete(
                businessId,
                """
                DELETE FROM refresh_tokens
                WHERE user_id IN (
                    SELECT user_id
                    FROM business_memberships
                    WHERE business_id = :businessId
                )
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM appointment_management_tokens
                WHERE appointment_id IN (
                    SELECT id
                    FROM appointments
                    WHERE business_id = :businessId
                )
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM appointment_reminders
                WHERE appointment_id IN (
                    SELECT id
                    FROM appointments
                    WHERE business_id = :businessId
                )
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM appointments
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM customers
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM professional_services
                WHERE professional_id IN (
                    SELECT id
                    FROM professionals
                    WHERE business_id = :businessId
                )
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM professional_availability
                WHERE professional_id IN (
                    SELECT id
                    FROM professionals
                    WHERE business_id = :businessId
                )
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM schedule_exceptions
                WHERE professional_id IN (
                    SELECT id
                    FROM professionals
                    WHERE business_id = :businessId
                )
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM business_settings
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM services
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM professionals
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM business_memberships
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                businessId,
                """
                DELETE FROM businesses
                WHERE id = :businessId
                """
            );

            entityManager.flush();
            entityManager.clear();
        });
    }

    private void deleteUser(Long userId) {

        if (userId == null) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            entityManager
                .createNativeQuery(
                    "DELETE FROM refresh_tokens WHERE user_id = :userId"
                )
                .setParameter("userId", userId)
                .executeUpdate();

            entityManager
                .createNativeQuery(
                    "DELETE FROM users WHERE id = :userId"
                )
                .setParameter("userId", userId)
                .executeUpdate();

            entityManager.flush();
            entityManager.clear();
        });
    }

    private void executeDelete(
        Long businessId,
        String sql
    ) {

        assertTransactionUsesTestDatabase();

        entityManager
            .createNativeQuery(sql)
            .setParameter("businessId", businessId)
            .executeUpdate();
    }

    protected static String extractJsonString(
        String json,
        String field
    ) {

        String marker = "\"" + field + "\":\"";

        int start = json.indexOf(marker);

        if (start < 0) {
            throw new AssertionError(
                "La respuesta no contiene " + field + ": " + json
            );
        }

        start += marker.length();

        int end = json.indexOf('"', start);

        if (end < 0) {
            throw new AssertionError(
                "La respuesta corta el campo " + field + ": " + json
            );
        }

        return json.substring(start, end);
    }

    protected record OperationalBusiness(
        Long businessId,
        Long userId,
        String email,
        Long professionalId,
        Long serviceId,
        Long customerId,
        LocalDate bookingDate,
        OffsetDateTime startAt
    ) {
    }

    protected record MemberCredentials(
        Long userId,
        String email
    ) {
    }

    protected record Session(
        String accessToken,
        String refreshToken
    ) {
    }
}
