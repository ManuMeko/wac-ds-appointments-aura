package com.wacds.appointments.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretRulesTests {

    private static final String STRONG_SECRET =
        "prod-test-only-not-a-real-secret-9f3a7c1e5b";

    @Test
    void acceptsALongUnpublishedSecret() {
        assertDoesNotThrow(() ->
            JwtSecretRules.requireProductionSecret(STRONG_SECRET)
        );
    }

    @Test
    void rejectsAMissingSecret() {
        assertThrows(
            IllegalStateException.class,
            () -> JwtSecretRules.requireProductionSecret(" ")
        );
    }

    @Test
    void rejectsAShortSecret() {
        assertThrows(
            IllegalStateException.class,
            () -> JwtSecretRules.requireProductionSecret(
                "short-but-not-long-enough"
            )
        );
    }

    @Test
    void rejectsThePublishedDevelopmentSecret() {
        assertThrows(
            IllegalStateException.class,
            () -> JwtSecretRules.requireProductionSecret(
                JwtSecretRules.PUBLISHED_DEV_SECRET
            )
        );
    }

    @Test
    void rejectsThePublishedTestSecret() {
        assertThrows(
            IllegalStateException.class,
            () -> JwtSecretRules.requireProductionSecret(
                "  " + JwtSecretRules.PUBLISHED_TEST_SECRET + "  "
            )
        );
    }

    @Test
    void rejectsARepeatedCharacterSecret() {
        assertThrows(
            IllegalStateException.class,
            () -> JwtSecretRules.requireProductionSecret(
                "a".repeat(48)
            )
        );
    }
}
