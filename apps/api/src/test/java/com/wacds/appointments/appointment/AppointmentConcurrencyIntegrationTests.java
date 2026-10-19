
package com.wacds.appointments.appointment;

import com.wacds.appointments.availability.ProfessionalAvailability;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.settings.BusinessSettings;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.professionalservice.ProfessionalServiceAssignment;
import com.wacds.appointments.publicbooking.PublicBookingService;
import com.wacds.appointments.publicbooking.dto.PublicBookingRequest;
import com.wacds.appointments.service.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Pruebas de concurrencia con PostgreSQL real.
 *
 * PRUEBA 1:
 * Comprueba la serializacion mediante FOR UPDATE.
 *
 * PRUEBA 2:
 * Ejecuta dos reservas reales simultaneas
 * contra PublicBookingService.createBooking().
 *
 * RESULTADO ESPERADO:
 *
 * - Una reserva creada.
 * - Una reserva rechazada por conflicto.
 * - Una unica cita persistida.
 *
 * AISLAMIENTO:
 *
 * Exclusivamente wac_ds_appointments_test.
 *
 * Se verifica current_database() antes
 * de escribir, dentro de las transacciones
 * y antes de eliminar los registros.
 *
 * No utilizar contra la base de desarrollo.
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
class AppointmentConcurrencyIntegrationTests {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String EXPECTED_DATABASE =
        "wac_ds_appointments_test";

    private static final String BUSINESS_TIMEZONE =
        "Europe/Madrid";

    private static final int LOCK_TIMEOUT_SECONDS = 8;

    private static final int EXECUTOR_SHUTDOWN_SECONDS = 8;

    private static final int BOOKING_TIMEOUT_SECONDS = 20;

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PublicBookingService publicBookingService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    // =========================================================
    // TEST STATE
    // =========================================================

    private TransactionTemplate transactionTemplate;

    private Long businessId;
    private Long professionalId;
    private Long serviceId;

    private LocalDate bookingDate;
    private OffsetDateTime bookingStartAt;

    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() throws Exception {

        assertIsolatedDatabase();

        transactionTemplate =
            new TransactionTemplate(transactionManager);

        createCompleteTestFixture();
    }

    // =========================================================
    // DATABASE PROTECTION
    // =========================================================

    private void assertIsolatedDatabase() throws Exception {

        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT current_database()"
            )
        ) {

            assertTrue(
                resultSet.next(),
                "PostgreSQL no devolvió current_database()."
            );

            assertEquals(
                EXPECTED_DATABASE,
                resultSet.getString(1),
                "OPERACIÓN BLOQUEADA: base de datos incorrecta."
            );
        }
    }

    private void assertTransactionUsesTestDatabase() {

        Object result = entityManager
            .createNativeQuery(
                "SELECT current_database()"
            )
            .getSingleResult();

        assertEquals(
            EXPECTED_DATABASE,
            String.valueOf(result),
            "OPERACIÓN BLOQUEADA: la transacción "
                + "no utiliza la base TEST."
        );
    }

    // =========================================================
    // COMPLETE FIXTURE
    // =========================================================

    /**
     * Prepara un negocio independiente con:
     *
     * - Business.
     * - BusinessSettings.
     * - Professional.
     * - Service.
     * - ProfessionalServiceAssignment.
     * - ProfessionalAvailability.
     *
     * La cita se solicita para mañana a las 11:00.
     */
    private void createCompleteTestFixture() {

        String suffix = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 16);

        ZoneId zone = ZoneId.of(BUSINESS_TIMEZONE);

        bookingDate = LocalDate.now(zone).plusDays(1);

        bookingStartAt = bookingDate
            .atTime(11, 0)
            .atZone(zone)
            .toOffsetDateTime();

        transactionTemplate.executeWithoutResult(status -> {

            assertTransactionUsesTestDatabase();

            // -------------------------------------------------
            // BUSINESS
            // -------------------------------------------------

            Business business = Business.builder()
                .name("WAC DS Concurrency Test")
                .slug("wac-ds-concurrency-" + suffix)
                .email(
                    "business-" + suffix + "@example.com"
                )
                .timezone(BUSINESS_TIMEZONE)
                .active(true)
                .publicBookingEnabled(true)
                .build();

            entityManager.persist(business);
            entityManager.flush();

            businessId = business.getId();

            // -------------------------------------------------
            // BUSINESS SETTINGS
            // -------------------------------------------------

            BusinessSettings settings =
                BusinessSettings.builder()
                    .business(business)
                    .minimumBookingNoticeMinutes(0)
                    .bookingHorizonDays(60)
                    .slotIntervalMinutes(15)
                    .defaultBufferBeforeMinutes(0)
                    .defaultBufferAfterMinutes(0)
                    .autoConfirmAppointments(true)
                    .remindersEnabled(false)
                    .secondReminderEnabled(false)
                    .build();

            entityManager.persist(settings);
            entityManager.flush();

            // -------------------------------------------------
            // PROFESSIONAL
            // -------------------------------------------------

            Professional professional =
                Professional.builder()
                    .business(business)
                    .firstName("Concurrency")
                    .lastName("Professional")
                    .displayName("Concurrency Professional")
                    .active(true)
                    .bookingEnabled(true)
                    .build();

            entityManager.persist(professional);
            entityManager.flush();

            professionalId = professional.getId();

            // -------------------------------------------------
            // SERVICE
            // -------------------------------------------------

            Service service = Service.builder()
                .business(business)
                .name("Concurrency Test Service")
                .slug("concurrency-service-" + suffix)
                .durationMinutes(30)
                .price(BigDecimal.ZERO)
                .active(true)
                .bookingEnabled(true)
                .build();

            entityManager.persist(service);
            entityManager.flush();

            serviceId = service.getId();

            // -------------------------------------------------
            // PROFESSIONAL SERVICE ASSIGNMENT
            // -------------------------------------------------

            ProfessionalServiceAssignment assignment =
                ProfessionalServiceAssignment.create(
                    professional,
                    service
                );

            entityManager.persist(assignment);
            entityManager.flush();

            // -------------------------------------------------
            // WEEKLY AVAILABILITY
            // -------------------------------------------------

            ProfessionalAvailability availability =
                ProfessionalAvailability.createActive(
                    professional,
                    bookingDate.getDayOfWeek(),
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0)
                );

            entityManager.persist(availability);
            entityManager.flush();
        });

        assertNotNull(businessId);
        assertNotNull(professionalId);
        assertNotNull(serviceId);
        assertNotNull(bookingStartAt);
    }

    // =========================================================
    // TEST 1 - POSTGRESQL LOCK
    // =========================================================

    @Test
    @Timeout(value = 25, unit = TimeUnit.SECONDS)
    @DisplayName(
        "FOR UPDATE serializa las transacciones "
            + "sobre la agenda del profesional"
    )
    void professionalScheduleLockSerializesTransactions()
        throws Exception {

        assertIsolatedDatabase();

        CountDownLatch firstLockAcquired =
            new CountDownLatch(1);

        CountDownLatch releaseFirstTransaction =
            new CountDownLatch(1);

        CountDownLatch secondAttemptStarted =
            new CountDownLatch(1);

        ExecutorService executor =
            Executors.newFixedThreadPool(2);

        try {

            Future<Long> firstFuture =
                executor.submit(() ->
                    transactionTemplate.execute(status -> {

                        assertTransactionUsesTestDatabase();

                        Long lockedId =
                            appointmentRepository
                                .lockProfessionalSchedule(
                                    businessId,
                                    professionalId
                                )
                                .orElseThrow();

                        firstLockAcquired.countDown();

                        try {

                            boolean released =
                                releaseFirstTransaction.await(
                                    LOCK_TIMEOUT_SECONDS,
                                    TimeUnit.SECONDS
                                );

                            if (!released) {
                                throw new IllegalStateException(
                                    "Timeout liberando FOR UPDATE."
                                );
                            }

                        } catch (InterruptedException exception) {

                            Thread.currentThread().interrupt();

                            throw new IllegalStateException(
                                "Transacción interrumpida.",
                                exception
                            );
                        }

                        return lockedId;
                    })
                );

            assertTrue(
                firstLockAcquired.await(
                    LOCK_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                ),
                "La primera transacción no adquirió el bloqueo."
            );

            Future<Long> secondFuture =
                executor.submit(() ->
                    transactionTemplate.execute(status -> {

                        assertTransactionUsesTestDatabase();

                        secondAttemptStarted.countDown();

                        return appointmentRepository
                            .lockProfessionalSchedule(
                                businessId,
                                professionalId
                            )
                            .orElseThrow();
                    })
                );

            assertTrue(
                secondAttemptStarted.await(
                    LOCK_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                )
            );

            assertThrows(
                TimeoutException.class,
                () -> secondFuture.get(
                    400,
                    TimeUnit.MILLISECONDS
                ),
                "La segunda transacción no esperó "
                    + "a la liberación del bloqueo."
            );

            assertFalse(secondFuture.isDone());

            releaseFirstTransaction.countDown();

            assertEquals(
                professionalId,
                firstFuture.get(
                    LOCK_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                )
            );

            assertEquals(
                professionalId,
                secondFuture.get(
                    LOCK_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                )
            );

        } finally {

            releaseFirstTransaction.countDown();

            shutdownExecutor(executor);
        }
    }

    // =========================================================
    // TEST 2 - REAL CONCURRENT BOOKINGS
    // =========================================================

    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    @DisplayName(
        "Dos clientes simultaneos no pueden "
            + "reservar la misma franja"
    )
    void concurrentPublicBookingsCreateExactlyOneAppointment()
        throws Exception {

        assertIsolatedDatabase();

        // -----------------------------------------------------
        // INITIAL DATABASE STATE
        // -----------------------------------------------------

        long initialCount = countAppointmentsForSlot();

        assertEquals(
            0L,
            initialCount,
            "La franja de prueba ya contiene una reserva."
        );

        // -----------------------------------------------------
        // TWO DIFFERENT CUSTOMERS
        // -----------------------------------------------------

        PublicBookingRequest firstRequest =
            new PublicBookingRequest(
                businessId,
                professionalId,
                serviceId,
                bookingStartAt,
                "Concurrency",
                "Customer One",
                "customer-one-"
                    + UUID.randomUUID()
                    + "@example.com",
                "+34600000001",
                "Concurrent booking test A"
            );

        PublicBookingRequest secondRequest =
            new PublicBookingRequest(
                businessId,
                professionalId,
                serviceId,
                bookingStartAt,
                "Concurrency",
                "Customer Two",
                "customer-two-"
                    + UUID.randomUUID()
                    + "@example.com",
                "+34600000002",
                "Concurrent booking test B"
            );

        // -----------------------------------------------------
        // CONCURRENCY COORDINATION
        // -----------------------------------------------------

        CountDownLatch ready =
            new CountDownLatch(2);

        CountDownLatch start =
            new CountDownLatch(1);

        ExecutorService executor =
            Executors.newFixedThreadPool(2);

        BookingAttempt firstResult;
        BookingAttempt secondResult;

        try {

            Future<BookingAttempt> firstFuture =
                executor.submit(() ->
                    executeBookingAttempt(
                        firstRequest,
                        ready,
                        start
                    )
                );

            Future<BookingAttempt> secondFuture =
                executor.submit(() ->
                    executeBookingAttempt(
                        secondRequest,
                        ready,
                        start
                    )
                );

            assertTrue(
                ready.await(
                    BOOKING_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                ),
                "Los dos clientes no llegaron "
                    + "al punto de salida."
            );

            /*
             * Ambos hilos reciben la señal
             * de inicio de manera simultánea.
             */
            start.countDown();

            firstResult = firstFuture.get(
                BOOKING_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            );

            secondResult = secondFuture.get(
                BOOKING_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            );

        } finally {

            start.countDown();

            shutdownExecutor(executor);
        }

        // -----------------------------------------------------
        // RESULT CLASSIFICATION
        // -----------------------------------------------------

        int successfulBookings =
            (firstResult.success() ? 1 : 0)
                + (secondResult.success() ? 1 : 0);

        int conflicts =
            (firstResult.conflict() ? 1 : 0)
                + (secondResult.conflict() ? 1 : 0);

        assertEquals(
            1,
            successfulBookings,
            "Debe crearse exactamente una reserva. "
                + "Resultado A: " + firstResult
                + "; resultado B: " + secondResult
        );

        assertEquals(
            1,
            conflicts,
            "La segunda petición debe rechazarse "
                + "con ConflictException. "
                + "Resultado A: " + firstResult
                + "; resultado B: " + secondResult
        );

        // -----------------------------------------------------
        // DATABASE ASSERTION
        // -----------------------------------------------------

        long persistedCount = countAppointmentsForSlot();

        assertEquals(
            1L,
            persistedCount,
            "PostgreSQL contiene reservas duplicadas "
                + "para el mismo profesional y horario."
        );
    }

    // =========================================================
    // BOOKING ATTEMPT
    // =========================================================

    /**
     * Ejecuta la misma operacion publica
     * utilizada por el backend real.
     *
     * No captura errores inesperados
     * como conflictos validos.
     */
    private BookingAttempt executeBookingAttempt(
        PublicBookingRequest request,
        CountDownLatch ready,
        CountDownLatch start
    ) {

        ready.countDown();

        try {

            boolean started = start.await(
                BOOKING_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            );

            if (!started) {
                return new BookingAttempt(
                    false,
                    false,
                    "Timeout esperando inicio."
                );
            }

            /*
             * Verificación explícita antes
             * de entrar en createBooking().
             */
            assertIsolatedDatabase();

            /*
             * PublicBookingService crea
             * su propia transacción.
             *
             * El bloqueo de agenda se adquiere
             * desde el servicio real.
             */
            Object response =
                publicBookingService.createBooking(request);

            assertNotNull(
                response,
                "createBooking devolvió null."
            );

            return new BookingAttempt(
                true,
                false,
                "Reserva creada."
            );

        } catch (ConflictException exception) {

            return new BookingAttempt(
                false,
                true,
                exception.getMessage()
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            return new BookingAttempt(
                false,
                false,
                "Hilo interrumpido: " + exception
            );

        } catch (Exception exception) {

            return new BookingAttempt(
                false,
                false,
                exception.getClass().getSimpleName()
                    + ": " + exception.getMessage()
            );
        }
    }

    // =========================================================
    // DATABASE ASSERTIONS
    // =========================================================

    private long countAppointmentsForSlot() {

        Long count = transactionTemplate.execute(status -> {

            assertTransactionUsesTestDatabase();

            Number result = (Number) entityManager
                .createNativeQuery(
                    """
                    SELECT COUNT(*)
                    FROM appointments
                    WHERE business_id = :businessId
                      AND professional_id = :professionalId
                      AND start_at = :startAt
                    """
                )
                .setParameter("businessId", businessId)
                .setParameter("professionalId", professionalId)
                .setParameter("startAt", bookingStartAt)
                .getSingleResult();

            return result.longValue();
        });

        assertNotNull(count);

        return count;
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    /**
     * Elimina solo los registros del negocio
     * creado por esta clase.
     *
     * Orden:
     *
     * 1. Management tokens.
     * 2. Appointment reminders.
     * 3. Appointments.
     * 4. Customers.
     * 5. Professional/service assignments.
     * 6. Professional availability.
     * 7. Business settings.
     * 8. Services.
     * 9. Professionals.
     * 10. Business.
     *
     * No se utilizan TRUNCATE ni borrados globales.
     */
    @AfterEach
    void cleanUp() throws Exception {

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
                DELETE FROM businesses
                WHERE id = :businessId
                """
            );

            entityManager.flush();
            entityManager.clear();
        });
    }

    /**
     * Ejecuta un DELETE limitado al negocio
     * temporal creado por el test.
     */
    private void executeDelete(String sql) {

        assertTransactionUsesTestDatabase();

        entityManager
            .createNativeQuery(sql)
            .setParameter("businessId", businessId)
            .executeUpdate();
    }

    // =========================================================
    // EXECUTOR SHUTDOWN
    // =========================================================

    private void shutdownExecutor(
        ExecutorService executor
    ) throws InterruptedException {

        executor.shutdown();

        if (
            !executor.awaitTermination(
                EXECUTOR_SHUTDOWN_SECONDS,
                TimeUnit.SECONDS
            )
        ) {

            executor.shutdownNow();

            assertTrue(
                executor.awaitTermination(
                    3,
                    TimeUnit.SECONDS
                ),
                "Los hilos de concurrencia "
                    + "no finalizaron correctamente."
            );
        }
    }

    // =========================================================
    // INTERNAL RESULT
    // =========================================================

    private record BookingAttempt(
        boolean success,
        boolean conflict,
        String detail
    ) {
    }
}
