package com.wacds.appointments.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Ventana fija en memoria. No se comparte entre réplicas
 * ni sobrevive a un reinicio del proceso.
 */
public final class FixedWindowRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final LongSupplier nowMillis;

    private final ConcurrentHashMap<String, Window> windows =
        new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(LongSupplier nowMillis) {
        this.nowMillis = nowMillis;
    }

    public Attempt tryAcquire(
        String key,
        int limit,
        long windowMillis
    ) {
        if (limit < 1) {
            throw new IllegalArgumentException(
                "limit debe ser mayor que cero."
            );
        }

        if (windowMillis < 1) {
            throw new IllegalArgumentException(
                "windowMillis debe ser mayor que cero."
            );
        }

        long now = nowMillis.getAsLong();

        Window window = windows.compute(key, (ignored, current) -> {
            if (
                current == null
                    || now - current.startedAtMillis >= windowMillis
            ) {
                return new Window(now);
            }

            current.count.incrementAndGet();
            return current;
        });

        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(entry ->
                now - entry.getValue().startedAtMillis >= windowMillis
            );
        }

        if (window.count.get() <= limit) {
            return new Attempt(true, 0);
        }

        long remainingMillis = windowMillis - (now - window.startedAtMillis);
        int retryAfterSeconds = (int) Math.max(
            1,
            (remainingMillis + 999) / 1000
        );

        return new Attempt(false, retryAfterSeconds);
    }

    public record Attempt(
        boolean allowed,
        int retryAfterSeconds
    ) {
    }

    private static final class Window {

        private final long startedAtMillis;

        private final AtomicInteger count = new AtomicInteger(1);

        private Window(long startedAtMillis) {
            this.startedAtMillis = startedAtMillis;
        }
    }
}
