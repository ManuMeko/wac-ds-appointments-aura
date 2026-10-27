package com.wacds.appointments.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita el login y la creación de reservas públicas
 * por dirección remota.
 *
 * No lee X-Forwarded-For. Con
 * server.forward-headers-strategy=framework, Spring Boot
 * reescribe getRemoteAddr() antes de este filtro, y solo
 * entonces esa dirección es la del cliente.
 */
@Component
public class PublicEndpointRateLimitFilter
    extends OncePerRequestFilter
    implements Ordered {

    private static final Logger log =
        LoggerFactory.getLogger(PublicEndpointRateLimitFilter.class);

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private static final String PUBLIC_BOOKING_PATH =
        "/api/v1/public/bookings";

    private final boolean enabled;

    private final int loginLimit;

    private final int bookingLimit;

    private final long windowMillis;

    private final FixedWindowRateLimiter limiter;

    @Autowired
    public PublicEndpointRateLimitFilter(
        @Value("${app.security.rate-limit.enabled:false}")
        boolean enabled,

        @Value("${app.security.rate-limit.login-per-minute:10}")
        int loginLimit,

        @Value("${app.security.rate-limit.public-booking-per-minute:10}")
        int bookingLimit,

        @Value("${app.security.rate-limit.window-seconds:60}")
        long windowSeconds
    ) {
        this(
            enabled,
            loginLimit,
            bookingLimit,
            windowSeconds,
            System::currentTimeMillis
        );
    }

    PublicEndpointRateLimitFilter(
        boolean enabled,
        int loginLimit,
        int bookingLimit,
        long windowSeconds,
        LongSupplier nowMillis
    ) {
        if (
            enabled
                && (
                    loginLimit < 1
                        || bookingLimit < 1
                        || windowSeconds < 1
                )
        ) {
            throw new IllegalStateException(
                "Los limites de app.security.rate-limit "
                    + "deben ser mayores que cero."
            );
        }

        this.enabled = enabled;
        this.loginLimit = loginLimit;
        this.bookingLimit = bookingLimit;
        this.windowMillis = windowSeconds * 1000L;
        this.limiter = new FixedWindowRateLimiter(nowMillis);
    }

    @Override
    public int getOrder() {
        // ForwardedHeaderFilter queda en HIGHEST_PRECEDENCE.
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || bucketFor(request) == null;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        Bucket bucket = bucketFor(request);

        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = bucket == Bucket.LOGIN
            ? loginLimit
            : bookingLimit;

        String clientAddress = clientAddress(request);

        FixedWindowRateLimiter.Attempt attempt = limiter.tryAcquire(
            bucket.name() + "|" + clientAddress,
            limit,
            windowMillis
        );

        if (attempt.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn(
            "Rate limit en {} desde {}",
            requestPath(request),
            clientAddress
        );

        response.setStatus(429);
        response.setHeader(
            "Retry-After",
            Integer.toString(attempt.retryAfterSeconds())
        );
        response.setHeader("Cache-Control", "no-store");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            """
            {
              "status": 429,
              "error": "Too Many Requests",
              "code": "RATE_LIMITED",
              "message": "Demasiadas solicitudes. Espera un momento e inténtalo de nuevo."
            }
            """
        );
    }

    private static Bucket bucketFor(HttpServletRequest request) {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = requestPath(request);

        if (LOGIN_PATH.equals(path)) {
            return Bucket.LOGIN;
        }

        if (PUBLIC_BOOKING_PATH.equals(path)) {
            return Bucket.BOOKING;
        }

        return null;
    }

    private static String requestPath(HttpServletRequest request) {

        String path = request.getRequestURI();

        if (path == null) {
            return "";
        }

        String contextPath = request.getContextPath();

        if (
            contextPath != null
                && !contextPath.isEmpty()
                && path.startsWith(contextPath)
        ) {
            path = path.substring(contextPath.length());
        }

        int query = path.indexOf('?');

        if (query >= 0) {
            path = path.substring(0, query);
        }

        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    private static String clientAddress(HttpServletRequest request) {

        String remoteAddress = request.getRemoteAddr();

        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }

        return remoteAddress;
    }

    private enum Bucket {
        LOGIN,
        BOOKING
    }
}
