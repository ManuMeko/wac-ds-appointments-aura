
package com.wacds.appointments;

import com.wacds.appointments.publicbooking.management.AppointmentManagementToken;
import com.wacds.appointments.publicbooking.management.AppointmentManagementTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Pruebas generales de seguridad y arranque.
 *
 * OBJETIVOS:
 *
 * 1. Verificar el arranque de Spring Boot.
 * 2. Comprobar el aislamiento de PostgreSQL.
 * 3. Validar hashes SHA-256.
 * 4. Rechazar tokens invalidos.
 * 5. Comprobar la expiracion.
 * 6. Validar revocaciones.
 * 7. Comprobar el ultimo uso.
 *
 * AISLAMIENTO:
 *
 * Todas las pruebas deben utilizar:
 *
 * wac_ds_appointments_test
 *
 * Nunca:
 *
 * wac_ds_appointments
 *
 * SEGURIDAD:
 *
 * - Se activa expresamente el perfil test.
 * - Se comprueba current_database().
 * - Se bloquea la ejecucion si la BD no coincide.
 * - No se crean reservas.
 * - No se modifican registros de negocio.
 * - No se eliminan registros existentes.
 *
 * Las pruebas de concurrencia real
 * se encuentran en:
 *
 * AppointmentConcurrencyIntegrationTests.
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppointmentsApiApplicationTests {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String EXPECTED_DATABASE =
        "wac_ds_appointments_test";

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    @Autowired
    private AppointmentManagementTokenService tokenService;

    @Autowired
    private DataSource dataSource;

    // =========================================================
    // DATABASE ISOLATION
    // =========================================================

    /**
     * Se ejecuta antes de cada prueba.
     *
     * Si Spring Boot conecta con una base distinta
     * de wac_ds_appointments_test, se detiene
     * inmediatamente la prueba.
     */
    @BeforeEach
    void verifyDatabaseIsolation() throws Exception {

        assertNotNull(
            dataSource,
            "El DataSource de pruebas no esta disponible."
        );

        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT current_database()"
            )
        ) {

            assertTrue(
                resultSet.next(),
                "PostgreSQL no devolvio el nombre "
                    + "de la base de datos."
            );

            String actualDatabase = resultSet.getString(1);

            assertEquals(
                EXPECTED_DATABASE,
                actualDatabase,
                "OPERACION BLOQUEADA: las pruebas "
                    + "estan conectadas a una base "
                    + "de datos incorrecta."
            );
        }
    }

    // =========================================================
    // SPRING BOOT CONTEXT
    // =========================================================

    @Test
    @DisplayName(
        "El contexto Spring Boot arranca "
            + "correctamente con el perfil test"
    )
    void contextLoads() {

        assertNotNull(tokenService);
        assertNotNull(dataSource);
    }

    // =========================================================
    // SHA-256
    // =========================================================

    @Test
    @DisplayName(
        "Un mismo token produce siempre "
            + "el mismo SHA-256"
    )
    void sameTokenProducesSameHash() {

        String token =
            "abcdefghijklmnopqrstuvwxyzABCDEFG1234567890";

        String firstHash = tokenService.hashToken(token);
        String secondHash = tokenService.hashToken(token);

        assertEquals(firstHash, secondHash);

        assertEquals(64, firstHash.length());

        assertTrue(
            firstHash.matches("^[a-f0-9]{64}$")
        );
    }

    @Test
    @DisplayName(
        "Dos tokens diferentes producen hashes diferentes"
    )
    void differentTokensProduceDifferentHashes() {

        String firstToken =
            "abcdefghijklmnopqrstuvwxyzABCDEFG1234567890";

        String secondToken =
            "abcdefghijklmnopqrstuvwxyzABCDEFG1234567891";

        String firstHash =
            tokenService.hashToken(firstToken);

        String secondHash =
            tokenService.hashToken(secondToken);

        assertNotEquals(firstHash, secondHash);
    }

    @Test
    @DisplayName("El sistema rechaza tokens vacios")
    void rejectsEmptyTokens() {

        assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.hashToken("")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.hashToken(null)
        );
    }

    @Test
    @DisplayName(
        "El sistema rechaza tokens "
            + "con caracteres invalidos"
    )
    void rejectsInvalidTokenCharacters() {

        String invalidToken =
            "abcdefghijklmnopqrstuvwxyzABCDEFG123456789!";

        assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.hashToken(invalidToken)
        );
    }

    @Test
    @DisplayName(
        "El sistema rechaza tokens demasiado cortos"
    )
    void rejectsShortTokens() {

        assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.hashToken("abc123")
        );
    }

    @Test
    @DisplayName(
        "El sistema rechaza tokens que contienen espacios"
    )
    void rejectsTokenWithWhitespace() {

        String tokenWithWhitespace =
            " abcdefghijklmnopqrstuvwxyzABCDEFG1234567890 ";

        assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.hashToken(tokenWithWhitespace)
        );
    }

    // =========================================================
    // EXPIRATION
    // =========================================================

    @Test
    @DisplayName(
        "Una credencial futura no esta expirada"
    )
    void futureTokenIsNotExpired() {

        OffsetDateTime now = OffsetDateTime.now();

        AppointmentManagementToken token =
            AppointmentManagementToken.builder()
                .expiresAt(now.plusDays(10))
                .build();

        assertFalse(token.isExpired(now));
        assertTrue(token.isUsable(now));
    }

    @Test
    @DisplayName(
        "Una credencial pasada esta expirada"
    )
    void pastTokenIsExpired() {

        OffsetDateTime now = OffsetDateTime.now();

        AppointmentManagementToken token =
            AppointmentManagementToken.builder()
                .expiresAt(now.minusMinutes(1))
                .build();

        assertTrue(token.isExpired(now));
        assertFalse(token.isUsable(now));
    }

    @Test
    @DisplayName(
        "La credencial expira exactamente en expiresAt"
    )
    void tokenExpiresAtExactDeadline() {

        OffsetDateTime expiration =
            OffsetDateTime.now().plusDays(1);

        AppointmentManagementToken token =
            AppointmentManagementToken.builder()
                .expiresAt(expiration)
                .build();

        assertTrue(token.isExpired(expiration));

        assertFalse(token.isUsable(expiration));
    }

    // =========================================================
    // REVOCATION
    // =========================================================

    @Test
    @DisplayName(
        "Revocar una credencial la vuelve inutilizable"
    )
    void revokedTokenIsNotUsable() {

        OffsetDateTime now = OffsetDateTime.now();

        AppointmentManagementToken token =
            AppointmentManagementToken.builder()
                .createdAt(now.minusDays(1))
                .expiresAt(now.plusDays(10))
                .build();

        assertTrue(token.isUsable(now));

        token.revoke(now);

        assertTrue(token.isRevoked());
        assertFalse(token.isUsable(now));
    }

    @Test
    @DisplayName(
        "La revocacion conserva la fecha original"
    )
    void revocationIsIdempotent() {

        OffsetDateTime createdAt =
            OffsetDateTime.now().minusDays(2);

        OffsetDateTime firstRevocation =
            createdAt.plusDays(1);

        AppointmentManagementToken token =
            AppointmentManagementToken.builder()
                .createdAt(createdAt)
                .build();

        token.revoke(firstRevocation);
        token.revoke(firstRevocation.plusHours(2));

        assertEquals(
            firstRevocation,
            token.getRevokedAt()
        );
    }

    @Test
    @DisplayName(
        "Una credencial revocada no admite nuevos usos"
    )
    void revokedTokenCannotBeMarkedUsed() {

        OffsetDateTime now = OffsetDateTime.now();

        AppointmentManagementToken token =
            AppointmentManagementToken.builder()
                .createdAt(now.minusDays(1))
                .expiresAt(now.plusDays(10))
                .build();

        token.revoke(now);

        assertThrows(
            IllegalStateException.class,
            () -> token.markUsed(now.plusMinutes(1))
        );
    }

    // =========================================================
    // LAST USED
    // =========================================================

    @Test
    @DisplayName(
        "lastUsedAt no puede retroceder"
    )
    void lastUsedAtCannotMoveBackwards() {

        OffsetDateTime now = OffsetDateTime.now();

        AppointmentManagementToken token =
            AppointmentManagementToken.builder()
                .createdAt(now.minusDays(1))
                .expiresAt(now.plusDays(10))
                .build();

        token.markUsed(now);
        token.markUsed(now.minusMinutes(5));

        assertEquals(
            now,
            token.getLastUsedAt()
        );
    }

    @Test
    @DisplayName(
        "No se admite el uso de un token expirado"
    )
    void expiredTokenCannotBeMarkedUsed() {

        OffsetDateTime now = OffsetDateTime.now();

        AppointmentManagementToken token =
            AppointmentManagementToken.builder()
                .createdAt(now.minusDays(10))
                .expiresAt(now.minusDays(1))
                .build();

        assertThrows(
            IllegalStateException.class,
            () -> token.markUsed(now)
        );
    }
}
