package com.wacds.appointments.appointment;

import com.wacds.appointments.appointment.dto.AppointmentRequest;
import com.wacds.appointments.appointment.dto.AppointmentRescheduleRequest;
import com.wacds.appointments.appointment.dto.AppointmentResponse;
import com.wacds.appointments.appointment.dto.AppointmentStatusUpdateRequest;
import com.wacds.appointments.auth.membership.BusinessMembership;
import com.wacds.appointments.auth.membership.BusinessRole;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.availability.ProfessionalAvailability;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.customer.Customer;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignment;
import com.wacds.appointments.reminder.ReminderDeliveryService.DeliveryResult;
import com.wacds.appointments.reminder.ResendEmailReminderDeliveryService;
import com.wacds.appointments.service.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Avisos por email al crear, reprogramar o cancelar
 * una cita desde el panel.
 *
 * El envío se sustituye por un mock de
 * ResendEmailReminderDeliveryService, el mismo bean
 * que usa el flujo público. La entrega real queda
 * desactivada, igual que en el resto de pruebas.
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
@ActiveProfiles("test")
class AppointmentPanelEmailIntegrationTests {

    private static final String EXPECTED_DATABASE =
        "wac_ds_appointments_test";

    private static final String BUSINESS_TIMEZONE =
        "Europe/Madrid";

    private static final String BUSINESS_NAME =
        "WAC DS Panel Email";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ResendEmailReminderDeliveryService emailDelivery;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate transactionTemplate;

    private Long businessId;
    private Long userId;
    private Long customerId;
    private Long professionalId;
    private Long serviceId;

    private String ownerEmail;
    private String customerEmail;

    private OffsetDateTime startAt;
    private OffsetDateTime rescheduledStartAt;

    @BeforeEach
    void setUp() throws Exception {

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
                "panel-email-test"
            )
        );

        transactionTemplate =
            new TransactionTemplate(transactionManager);

        createFixture();
        authenticateOwner();
    }

    @AfterEach
    void cleanUp() throws Exception {

        SecurityContextHolder.clearContext();

        assertIsolatedDatabase();

        if (
            transactionTemplate == null
                || businessId == null
        ) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            executeDelete(
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
                """
                DELETE FROM appointments
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                """
                DELETE FROM customers
                WHERE business_id = :businessId
                """
            );

            executeDelete(
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
                """
                DELETE FROM business_settings
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                """
                DELETE FROM services
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                """
                DELETE FROM professionals
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                """
                DELETE FROM business_memberships
                WHERE business_id = :businessId
                """
            );

            executeDelete(
                """
                DELETE FROM businesses
                WHERE id = :businessId
                """
            );

            if (userId != null) {
                entityManager
                    .createNativeQuery(
                        "DELETE FROM users WHERE id = :userId"
                    )
                    .setParameter("userId", userId)
                    .executeUpdate();
            }

            entityManager.flush();
            entityManager.clear();
        });
    }

    @Test
    @DisplayName(
        "Crear una cita desde el panel envía la confirmación "
            + "con el enlace de gestión"
    )
    void createFromPanelSendsConfirmationEmail() {

        AppointmentResponse created =
            createAppointment(
                startAt
            );

        SentEmail email =
            captureSingleEmail();

        assertEquals(
            customerEmail,
            email.recipient()
        );

        assertTrue(
            email.subject().contains(
                "Test booking confirmation"
            )
        );

        assertTrue(
            email.subject().contains(
                BUSINESS_NAME
            )
        );

        assertEquals(
            "panel-appointment-confirmation/"
                + created.id(),
            email.idempotencyKey()
        );

        assertTrue(
            email.html().contains(
                "Hemos recibido tu reserva"
            )
        );

        assertTrue(
            email.html().contains(
                "Panel Email Service"
            )
        );

        assertTrue(
            email.html().contains(
                "11:00"
            )
        );

        assertTrue(
            email.html().contains(
                "/reservar/gestionar?appointmentId="
                    + created.id()
                    + "#token="
            )
        );

        assertTrue(
            email.html().contains(
                "Gestionar mi cita"
            )
        );
    }

    @Test
    @DisplayName(
        "Reprogramar una cita desde el panel envía el nuevo horario"
    )
    void rescheduleFromPanelSendsUpdatedScheduleEmail() {

        AppointmentResponse created =
            createAppointment(
                startAt
            );

        clearInvocations(
            emailDelivery
        );

        AppointmentResponse rescheduled =
            appointmentService.reschedule(
                created.id(),
                new AppointmentRescheduleRequest(
                    rescheduledStartAt
                )
            );

        SentEmail email =
            captureSingleEmail();

        assertEquals(
            customerEmail,
            email.recipient()
        );

        assertEquals(
            created.id(),
            rescheduled.id()
        );

        assertTrue(
            email.subject().contains(
                "Tu cita ha cambiado de horario"
            )
        );

        assertTrue(
            email.html().contains(
                "Este es el nuevo horario."
            )
        );

        assertTrue(
            email.html().contains(
                "16:00"
            )
        );

        assertFalse(
            email.html().contains(
                "11:00"
            )
        );

        assertTrue(
            email.idempotencyKey().startsWith(
                "panel-appointment-reschedule/"
                    + created.id()
                    + "/"
            )
        );

        assertTrue(
            email.html().contains(
                "Si ya dispones de un enlace de gestión"
            )
        );
    }

    @Test
    @DisplayName(
        "Cancelar una cita desde el panel envía el aviso de cancelación"
    )
    void cancelFromPanelSendsCancellationEmail() {

        AppointmentResponse created =
            createAppointment(
                startAt
            );

        clearInvocations(
            emailDelivery
        );

        AppointmentResponse cancelled =
            appointmentService.updateStatus(
                created.id(),
                new AppointmentStatusUpdateRequest(
                    Appointment.AppointmentStatus.CANCELLED
                )
            );

        SentEmail email =
            captureSingleEmail();

        assertEquals(
            Appointment.AppointmentStatus.CANCELLED,
            cancelled.status()
        );

        assertEquals(
            customerEmail,
            email.recipient()
        );

        assertTrue(
            email.subject().contains(
                "Tu cita ha sido cancelada"
            )
        );

        assertTrue(
            email.html().contains(
                "ha quedado cancelada"
            )
        );

        assertTrue(
            email.html().contains(
                "11:00"
            )
        );

        assertEquals(
            "panel-appointment-cancellation/"
                + created.id(),
            email.idempotencyKey()
        );
    }

    @Test
    @DisplayName(
        "Un fallo al enviar el email no deshace la cita"
    )
    void emailFailureDoesNotRollBackAppointment() {

        when(
            emailDelivery.sendTransactionalEmail(
                anyString(),
                anyString(),
                anyString(),
                anyString()
            )
        ).thenThrow(
            new IllegalStateException(
                "resend down"
            )
        );

        AppointmentResponse created =
            createAppointment(
                startAt
            );

        assertNotNull(
            created.id()
        );

        assertEquals(
            Appointment.AppointmentStatus.CONFIRMED,
            appointmentRepository
                .findById(
                    created.id()
                )
                .orElseThrow()
                .getStatus()
        );

        verify(
            emailDelivery,
            times(1)
        ).sendTransactionalEmail(
            eq(customerEmail),
            anyString(),
            anyString(),
            eq(
                "panel-appointment-confirmation/"
                    + created.id()
            )
        );
    }

    @Test
    @DisplayName(
        "No se envía email si el cliente no tiene correo"
    )
    void createWithoutCustomerEmailDoesNotSend() {

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            Customer customer =
                entityManager.find(
                    Customer.class,
                    customerId
                );

            customer.setEmail(
                null
            );

            entityManager.flush();
        });

        AppointmentResponse created =
            createAppointment(
                startAt
            );

        assertNotNull(
            created.id()
        );

        verify(
            emailDelivery,
            never()
        ).sendTransactionalEmail(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        );
    }

    private SentEmail captureSingleEmail() {

        var recipient =
            org.mockito.ArgumentCaptor.forClass(
                String.class
            );

        var subject =
            org.mockito.ArgumentCaptor.forClass(
                String.class
            );

        var html =
            org.mockito.ArgumentCaptor.forClass(
                String.class
            );

        var idempotencyKey =
            org.mockito.ArgumentCaptor.forClass(
                String.class
            );

        verify(
            emailDelivery,
            times(1)
        ).sendTransactionalEmail(
            recipient.capture(),
            subject.capture(),
            html.capture(),
            idempotencyKey.capture()
        );

        return new SentEmail(
            recipient.getValue(),
            subject.getValue(),
            html.getValue(),
            idempotencyKey.getValue()
        );
    }

    private AppointmentResponse createAppointment(
        OffsetDateTime appointmentStart
    ) {

        return appointmentService.create(
            businessId,
            new AppointmentRequest(
                customerId,
                professionalId,
                serviceId,
                appointmentStart,
                Appointment.AppointmentStatus.CONFIRMED,
                null
            )
        );
    }

    private void authenticateOwner() {

        AuthenticatedUser principal =
            new AuthenticatedUser(
                userId,
                ownerEmail,
                businessId,
                BusinessRole.OWNER
            );

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
            )
        );
    }

    private void createFixture() {

        String suffix = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 16);

        ZoneId zone = ZoneId.of(BUSINESS_TIMEZONE);

        LocalDate bookingDate =
            LocalDate.now(zone).plusDays(1);

        startAt = bookingDate
            .atTime(11, 0)
            .atZone(zone)
            .toOffsetDateTime();

        rescheduledStartAt = bookingDate
            .atTime(16, 0)
            .atZone(zone)
            .toOffsetDateTime();

        ownerEmail = "owner-" + suffix + "@example.com";
        customerEmail = "customer-" + suffix + "@example.com";

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            Business business = Business.builder()
                .name(BUSINESS_NAME)
                .slug("wac-ds-panel-email-" + suffix)
                .email("business-" + suffix + "@example.com")
                .timezone(BUSINESS_TIMEZONE)
                .active(true)
                .publicBookingEnabled(true)
                .build();

            entityManager.persist(business);
            entityManager.flush();

            businessId = business.getId();

            BusinessSettings settings =
                BusinessSettings.builder()
                    .business(business)
                    .minimumBookingNoticeMinutes(0)
                    .bookingHorizonDays(60)
                    .slotIntervalMinutes(15)
                    .defaultBufferBeforeMinutes(0)
                    .defaultBufferAfterMinutes(0)
                    .autoConfirmAppointments(true)
                    .remindersEnabled(true)
                    .firstReminderMinutesBefore(60)
                    .secondReminderEnabled(true)
                    .secondReminderMinutesBefore(30)
                    .build();

            entityManager.persist(settings);

            User owner = User.builder()
                .email(ownerEmail)
                .passwordHash("test-password-hash")
                .firstName("Panel")
                .lastName("Owner")
                .active(true)
                .build();

            entityManager.persist(owner);
            entityManager.flush();

            userId = owner.getId();

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
                .firstName("Panel")
                .lastName("Customer")
                .email(customerEmail)
                .phone("+34600000111")
                .active(true)
                .build();

            entityManager.persist(customer);
            entityManager.flush();

            customerId = customer.getId();

            Professional professional =
                Professional.builder()
                    .business(business)
                    .firstName("Panel")
                    .lastName("Professional")
                    .displayName("Panel Professional")
                    .active(true)
                    .bookingEnabled(true)
                    .build();

            entityManager.persist(professional);
            entityManager.flush();

            professionalId = professional.getId();

            Service service = Service.builder()
                .business(business)
                .name("Panel Email Service")
                .slug("panel-email-service-" + suffix)
                .durationMinutes(30)
                .price(BigDecimal.ZERO)
                .active(true)
                .bookingEnabled(true)
                .build();

            entityManager.persist(service);
            entityManager.flush();

            serviceId = service.getId();

            entityManager.persist(
                ProfessionalServiceAssignment.create(
                    professional,
                    service
                )
            );

            entityManager.persist(
                ProfessionalAvailability.createActive(
                    professional,
                    bookingDate.getDayOfWeek(),
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0)
                )
            );

            entityManager.flush();
        });

        assertNotNull(businessId);
        assertNotNull(userId);
        assertNotNull(customerId);
        assertNotNull(professionalId);
        assertNotNull(serviceId);
    }

    private void assertIsolatedDatabase() throws Exception {

        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT current_database()"
            )
        ) {

            assertTrue(resultSet.next());

            assertEquals(
                EXPECTED_DATABASE,
                resultSet.getString(1),
                "OPERACIÓN BLOQUEADA: base de datos incorrecta."
            );
        }
    }

    private void assertTransactionUsesTestDatabase() {

        Object result = entityManager
            .createNativeQuery("SELECT current_database()")
            .getSingleResult();

        assertEquals(
            EXPECTED_DATABASE,
            String.valueOf(result),
            "OPERACIÓN BLOQUEADA: la transacción no utiliza la base TEST."
        );
    }

    private void executeDelete(String sql) {

        assertTransactionUsesTestDatabase();

        entityManager
            .createNativeQuery(sql)
            .setParameter("businessId", businessId)
            .executeUpdate();
    }

    private record SentEmail(
        String recipient,
        String subject,
        String html,
        String idempotencyKey
    ) {
    }
}
