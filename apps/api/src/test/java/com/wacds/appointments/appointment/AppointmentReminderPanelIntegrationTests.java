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
import com.wacds.appointments.reminder.AppointmentReminder;
import com.wacds.appointments.reminder.AppointmentReminder.ReminderStatus;
import com.wacds.appointments.reminder.AppointmentReminder.ReminderType;
import com.wacds.appointments.reminder.AppointmentReminderRepository;
import com.wacds.appointments.reminder.AppointmentReminderWorker;
import com.wacds.appointments.reminder.AppointmentReminderWorker.ProcessingResult;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sincronización de recordatorios desde el panel Business
 * y barrera final del worker.
 *
 * Cubre crear, confirmar, reprogramar y cancelar a través
 * de AppointmentService, más el rechazo del worker cuando
 * la cita ya no coincide con el snapshot del recordatorio.
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
class AppointmentReminderPanelIntegrationTests {

    private static final String EXPECTED_DATABASE =
        "wac_ds_appointments_test";

    private static final String BUSINESS_TIMEZONE =
        "Europe/Madrid";

    private static final int FIRST_REMINDER_MINUTES =
        60;

    private static final int SECOND_REMINDER_MINUTES =
        30;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentReminderRepository reminderRepository;

    @Autowired
    private AppointmentReminderWorker reminderWorker;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
        "Crear una cita desde el panel programa FIRST y SECOND"
    )
    void createFromPanelSchedulesReminders() {

        AppointmentResponse created =
            createAppointment(
                startAt,
                Appointment.AppointmentStatus.CONFIRMED
            );

        List<AppointmentReminder> reminders =
            remindersOf(created.id());

        assertEquals(2, reminders.size());

        AppointmentReminder first =
            reminder(reminders, ReminderType.FIRST);

        AppointmentReminder second =
            reminder(reminders, ReminderType.SECOND);

        assertPendingFor(
            first,
            created.startAt(),
            FIRST_REMINDER_MINUTES
        );

        assertPendingFor(
            second,
            created.startAt(),
            SECOND_REMINDER_MINUTES
        );

        assertEquals(
            customerEmail,
            first.getDestination()
        );

        assertEquals(
            customerEmail,
            second.getDestination()
        );
    }

    @Test
    @DisplayName(
        "Confirmar una cita desde el panel conserva los recordatorios"
    )
    void confirmingFromPanelKeepsActiveReminders() {

        AppointmentResponse created =
            createAppointment(
                startAt,
                Appointment.AppointmentStatus.PENDING
            );

        AppointmentResponse confirmed =
            appointmentService.updateStatus(
                created.id(),
                new AppointmentStatusUpdateRequest(
                    Appointment.AppointmentStatus.CONFIRMED
                )
            );

        assertEquals(
            Appointment.AppointmentStatus.CONFIRMED,
            confirmed.status()
        );

        List<AppointmentReminder> reminders =
            remindersOf(created.id());

        assertEquals(2, reminders.size());

        assertPendingFor(
            reminder(reminders, ReminderType.FIRST),
            created.startAt(),
            FIRST_REMINDER_MINUTES
        );

        assertPendingFor(
            reminder(reminders, ReminderType.SECOND),
            created.startAt(),
            SECOND_REMINDER_MINUTES
        );
    }

    @Test
    @DisplayName(
        "Reprogramar desde el panel cancela la ocurrencia anterior y crea la nueva"
    )
    void rescheduleFromPanelReplacesReminders() {

        AppointmentResponse created =
            createAppointment(
                startAt,
                Appointment.AppointmentStatus.CONFIRMED
            );

        AppointmentResponse rescheduled =
            appointmentService.reschedule(
                created.id(),
                new AppointmentRescheduleRequest(
                    rescheduledStartAt
                )
            );

        assertTrue(
            rescheduled.startAt().isEqual(rescheduledStartAt)
        );

        List<AppointmentReminder> reminders =
            remindersOf(created.id());

        assertEquals(4, reminders.size());

        assertEquals(
            ReminderStatus.CANCELLED,
            reminderForStart(
                reminders,
                ReminderType.FIRST,
                startAt
            ).getStatus()
        );

        assertEquals(
            ReminderStatus.CANCELLED,
            reminderForStart(
                reminders,
                ReminderType.SECOND,
                startAt
            ).getStatus()
        );

        assertPendingFor(
            reminderForStart(
                reminders,
                ReminderType.FIRST,
                rescheduledStartAt
            ),
            rescheduledStartAt,
            FIRST_REMINDER_MINUTES
        );

        assertPendingFor(
            reminderForStart(
                reminders,
                ReminderType.SECOND,
                rescheduledStartAt
            ),
            rescheduledStartAt,
            SECOND_REMINDER_MINUTES
        );
    }

    @Test
    @DisplayName(
        "Cancelar desde el panel cancela los activos y conserva los enviados"
    )
    void cancelFromPanelCancelsActiveRemindersAndKeepsSent() {

        AppointmentResponse created =
            createAppointment(
                startAt,
                Appointment.AppointmentStatus.CONFIRMED
            );

        AppointmentReminder firstBefore =
            reminder(
                remindersOf(created.id()),
                ReminderType.FIRST
            );

        markSent(firstBefore.getId());

        AppointmentResponse cancelled =
            appointmentService.updateStatus(
                created.id(),
                new AppointmentStatusUpdateRequest(
                    Appointment.AppointmentStatus.CANCELLED
                )
            );

        assertEquals(
            Appointment.AppointmentStatus.CANCELLED,
            cancelled.status()
        );

        List<AppointmentReminder> reminders =
            remindersOf(created.id());

        AppointmentReminder first =
            reminder(reminders, ReminderType.FIRST);

        AppointmentReminder second =
            reminder(reminders, ReminderType.SECOND);

        assertEquals(ReminderStatus.SENT, first.getStatus());
        assertEquals("TEST-SENT", first.getProviderMessageId());
        assertNotNull(first.getSentAt());

        assertEquals(ReminderStatus.CANCELLED, second.getStatus());
        assertNull(second.getSentAt());
        assertNull(second.getProviderMessageId());
    }

    @Test
    @DisplayName(
        "El worker no envía un recordatorio si la cita fue cancelada fuera de la sincronización"
    )
    void workerDoesNotSendWhenAppointmentWasCancelled() {

        AppointmentResponse created =
            createAppointment(
                startAt,
                Appointment.AppointmentStatus.CONFIRMED
            );

        Long reminderId =
            reminder(
                remindersOf(created.id()),
                ReminderType.FIRST
            ).getId();

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            Appointment appointment =
                appointmentRepository
                    .findById(created.id())
                    .orElseThrow();

            appointment.setStatus(
                Appointment.AppointmentStatus.CANCELLED
            );

            appointmentRepository.saveAndFlush(appointment);
        });

        ProcessingResult result =
            reminderWorker.processReminder(reminderId);

        assertEquals(ProcessingResult.SKIPPED, result);

        AppointmentReminder reminder =
            reminderRepository
                .findById(reminderId)
                .orElseThrow();

        assertEquals(ReminderStatus.CANCELLED, reminder.getStatus());
        assertNull(reminder.getSentAt());
        assertNull(reminder.getProviderMessageId());
    }

    @Test
    @DisplayName(
        "El worker no envía un recordatorio si la hora de la cita ya cambió"
    )
    void workerDoesNotSendWhenAppointmentStartChanged() {

        AppointmentResponse created =
            createAppointment(
                startAt,
                Appointment.AppointmentStatus.CONFIRMED
            );

        Long reminderId =
            reminder(
                remindersOf(created.id()),
                ReminderType.FIRST
            ).getId();

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            Appointment appointment =
                appointmentRepository
                    .findById(created.id())
                    .orElseThrow();

            appointment.setStartAt(
                appointment.getStartAt().plusHours(2)
            );

            appointment.setEndAt(
                appointment.getEndAt().plusHours(2)
            );

            appointmentRepository.saveAndFlush(appointment);
        });

        ProcessingResult result =
            reminderWorker.processReminder(reminderId);

        assertEquals(ProcessingResult.SKIPPED, result);

        AppointmentReminder reminder =
            reminderRepository
                .findById(reminderId)
                .orElseThrow();

        assertEquals(ReminderStatus.CANCELLED, reminder.getStatus());
        assertFalse(
            reminder.getAppointmentStartAt().isEqual(
                startAt.plusHours(2)
            )
        );
        assertNull(reminder.getSentAt());
        assertNull(reminder.getProviderMessageId());
    }

    @Test
    @DisplayName(
        "El worker no descarta un recordatorio cuya cita sigue coincidiendo"
    )
    void workerKeepsReminderWhenAppointmentStillMatches() {

        AppointmentResponse created =
            createAppointment(
                startAt,
                Appointment.AppointmentStatus.CONFIRMED
            );

        Long reminderId =
            reminder(
                remindersOf(created.id()),
                ReminderType.FIRST
            ).getId();

        ProcessingResult result =
            reminderWorker.processReminder(reminderId);

        assertNotEquals(ProcessingResult.SKIPPED, result);

        AppointmentReminder reminder =
            reminderRepository
                .findById(reminderId)
                .orElseThrow();

        assertNotEquals(
            ReminderStatus.CANCELLED,
            reminder.getStatus()
        );

        assertTrue(
            reminder.getStatus() == ReminderStatus.SENT
                || reminder.getStatus() == ReminderStatus.FAILED
        );
    }

    private AppointmentResponse createAppointment(
        OffsetDateTime appointmentStart,
        Appointment.AppointmentStatus status
    ) {

        return appointmentService.create(
            businessId,
            new AppointmentRequest(
                customerId,
                professionalId,
                serviceId,
                appointmentStart,
                status,
                null
            )
        );
    }

    private List<AppointmentReminder> remindersOf(
        Long appointmentId
    ) {

        return reminderRepository
            .findAllByAppointmentIdOrderByAppointmentStartAtAscScheduledForAscIdAsc(
                appointmentId
            );
    }

    private AppointmentReminder reminder(
        List<AppointmentReminder> reminders,
        ReminderType type
    ) {

        return reminders
            .stream()
            .filter(item -> item.getReminderType() == type)
            .findFirst()
            .orElseThrow();
    }

    private AppointmentReminder reminderForStart(
        List<AppointmentReminder> reminders,
        ReminderType type,
        OffsetDateTime appointmentStart
    ) {

        return reminders
            .stream()
            .filter(item ->
                item.getReminderType() == type
                    && item.getAppointmentStartAt() != null
                    && item.getAppointmentStartAt().isEqual(
                        appointmentStart
                    )
            )
            .findFirst()
            .orElseThrow();
    }

    private void assertPendingFor(
        AppointmentReminder reminder,
        OffsetDateTime appointmentStart,
        int minutesBefore
    ) {

        assertEquals(ReminderStatus.PENDING, reminder.getStatus());
        assertTrue(
            reminder.getAppointmentStartAt().isEqual(
                appointmentStart
            )
        );
        assertTrue(
            reminder.getScheduledFor().isEqual(
                appointmentStart.minusMinutes(minutesBefore)
            )
        );
        assertEquals(0, reminder.getAttemptCount());
        assertNull(reminder.getSentAt());
    }

    private void markSent(Long reminderId) {

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            AppointmentReminder reminder =
                reminderRepository
                    .findById(reminderId)
                    .orElseThrow();

            reminder.markSent(
                OffsetDateTime.now(),
                "TEST-SENT"
            );

            reminderRepository.saveAndFlush(reminder);
        });
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
                .name("WAC DS Reminder Panel")
                .slug("wac-ds-reminder-" + suffix)
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
                    .firstReminderMinutesBefore(
                        FIRST_REMINDER_MINUTES
                    )
                    .secondReminderEnabled(true)
                    .secondReminderMinutesBefore(
                        SECOND_REMINDER_MINUTES
                    )
                    .build();

            entityManager.persist(settings);

            User owner = User.builder()
                .email(ownerEmail)
                .passwordHash("test-password-hash")
                .firstName("Reminder")
                .lastName("Owner")
                .active(true)
                .build();

            entityManager.persist(owner);
            entityManager.flush();

            userId = owner.getId();

            BusinessMembership membership =
                BusinessMembership.builder()
                    .business(business)
                    .user(owner)
                    .role(BusinessRole.OWNER)
                    .active(true)
                    .build();

            entityManager.persist(membership);

            Customer customer = Customer.builder()
                .business(business)
                .firstName("Reminder")
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
                    .firstName("Reminder")
                    .lastName("Professional")
                    .displayName("Reminder Professional")
                    .active(true)
                    .bookingEnabled(true)
                    .build();

            entityManager.persist(professional);
            entityManager.flush();

            professionalId = professional.getId();

            Service service = Service.builder()
                .business(business)
                .name("Reminder Panel Service")
                .slug("reminder-service-" + suffix)
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
}
