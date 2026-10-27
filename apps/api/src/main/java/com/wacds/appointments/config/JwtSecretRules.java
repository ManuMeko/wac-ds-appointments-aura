package com.wacds.appointments.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Reglas del secreto HMAC cuando el perfil activo es prod.
 *
 * El valor de desarrollo publicado en application.properties
 * supera los 32 bytes, así que la comprobación general de
 * longitud no basta para impedir que producción arranque
 * firmando con ese secreto.
 */
public final class JwtSecretRules {

    public static final String PUBLISHED_DEV_SECRET =
        "wac-ds-appointments-dev-secret-change-me-2026-very-long-key";

    public static final String PUBLISHED_TEST_SECRET =
        "wac-ds-appointments-test-secret-isolated-2026-long-key";

    static final int MINIMUM_BYTES = 32;

    private static final int MINIMUM_DISTINCT_CHARACTERS = 8;

    private static final String PUBLISHED_MARKER = "change-me";

    private JwtSecretRules() {
    }

    public static void requireProductionSecret(String jwtSecret) {

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET es obligatorio en produccion."
            );
        }

        String trimmed = jwtSecret.trim();

        byte[] secretBytes = trimmed.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < MINIMUM_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET debe tener al menos "
                    + MINIMUM_BYTES
                    + " bytes."
            );
        }

        if (
            trimmed.equalsIgnoreCase(PUBLISHED_DEV_SECRET)
                || trimmed.equalsIgnoreCase(PUBLISHED_TEST_SECRET)
                || trimmed.toLowerCase(Locale.ROOT).contains(PUBLISHED_MARKER)
        ) {
            throw new IllegalStateException(
                "JWT_SECRET de produccion no puede ser un valor "
                    + "de desarrollo publicado en el repositorio."
            );
        }

        long distinctCharacters = trimmed.codePoints().distinct().count();

        if (distinctCharacters < MINIMUM_DISTINCT_CHARACTERS) {
            throw new IllegalStateException(
                "JWT_SECRET de produccion es demasiado predecible."
            );
        }
    }
}
