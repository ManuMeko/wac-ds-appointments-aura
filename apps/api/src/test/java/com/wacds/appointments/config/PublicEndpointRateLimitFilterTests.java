package com.wacds.appointments.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicEndpointRateLimitFilterTests {

    @Test
    void limitsLoginSeparatelyFromPublicBooking()
        throws ServletException, IOException {

        AtomicLong now = new AtomicLong(0);
        PublicEndpointRateLimitFilter filter = filter(true, now);

        assertEquals(200, post(filter, "/api/v1/auth/login", "203.0.113.10"));
        assertEquals(200, post(filter, "/api/v1/auth/login", "203.0.113.10"));
        assertEquals(429, post(filter, "/api/v1/auth/login", "203.0.113.10"));

        assertEquals(
            200,
            post(filter, "/api/v1/public/bookings", "203.0.113.10")
        );
        assertEquals(
            200,
            post(filter, "/api/v1/auth/login", "203.0.113.11")
        );
    }

    @Test
    void doesNotLimitBookingManagement()
        throws ServletException, IOException {

        AtomicLong now = new AtomicLong(0);
        PublicEndpointRateLimitFilter filter = filter(true, now);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertEquals(
                200,
                post(
                    filter,
                    "/api/v1/public/bookings/manage/cancel",
                    "203.0.113.10"
                )
            );
        }
    }

    @Test
    void opensANewWindowAfterTheInterval()
        throws ServletException, IOException {

        AtomicLong now = new AtomicLong(0);
        PublicEndpointRateLimitFilter filter = filter(true, now);

        assertEquals(200, post(filter, "/api/v1/public/bookings", "203.0.113.8"));
        assertEquals(200, post(filter, "/api/v1/public/bookings", "203.0.113.8"));
        assertEquals(429, post(filter, "/api/v1/public/bookings", "203.0.113.8"));

        now.set(60_000);

        assertEquals(200, post(filter, "/api/v1/public/bookings", "203.0.113.8"));
    }

    @Test
    void staysOpenWhenDisabled() throws ServletException, IOException {

        AtomicLong now = new AtomicLong(0);
        PublicEndpointRateLimitFilter filter = filter(false, now);
        AtomicInteger forwarded = new AtomicInteger();

        for (int attempt = 0; attempt < 5; attempt++) {
            MockHttpServletResponse response = dispatch(
                filter,
                "POST",
                "/api/v1/auth/login",
                "203.0.113.10",
                forwarded
            );

            assertEquals(200, response.getStatus());
        }

        assertEquals(5, forwarded.get());
    }

    @Test
    void ignoresASpoofedForwardedHeaderOnItsOwn()
        throws ServletException, IOException {

        AtomicLong now = new AtomicLong(0);
        PublicEndpointRateLimitFilter filter = limited(now);

        assertEquals(
            200,
            postWithHeader(filter, "10.0.0.2", "203.0.113.77")
        );
        assertEquals(
            429,
            postWithHeader(filter, "10.0.0.2", "198.51.100.8")
        );
        assertEquals(200, post(filter, "/api/v1/auth/login", "203.0.113.77"));
        assertTrue(
            filter.getOrder() > Ordered.HIGHEST_PRECEDENCE
        );
    }

    @Test
    void limitsTheAddressRewrittenByForwardedHeaderFilter()
        throws ServletException, IOException {

        AtomicLong now = new AtomicLong(0);
        PublicEndpointRateLimitFilter rateLimit = limited(now);
        ForwardedHeaderFilter forwardedHeaders = new ForwardedHeaderFilter();

        assertEquals(200, postBehindProxy(forwardedHeaders, rateLimit, "203.0.113.10"));
        assertEquals(200, postBehindProxy(forwardedHeaders, rateLimit, "203.0.113.11"));
        assertEquals(429, postBehindProxy(forwardedHeaders, rateLimit, "203.0.113.10"));
    }

    @Test
    void answersWithRetryAfter() throws ServletException, IOException {

        AtomicLong now = new AtomicLong(1_000);
        PublicEndpointRateLimitFilter filter = filter(true, now);

        post(filter, "/api/v1/auth/login", "203.0.113.9");
        post(filter, "/api/v1/auth/login", "203.0.113.9");

        MockHttpServletResponse limited = dispatch(
            filter,
            "POST",
            "/api/v1/auth/login",
            "203.0.113.9",
            new AtomicInteger()
        );

        assertEquals(429, limited.getStatus());
        assertEquals("60", limited.getHeader("Retry-After"));
        assertTrue(limited.getContentAsString().contains("RATE_LIMITED"));
    }

    private static PublicEndpointRateLimitFilter filter(
        boolean enabled,
        AtomicLong now
    ) {
        return new PublicEndpointRateLimitFilter(
            enabled,
            2,
            2,
            60,
            now::get
        );
    }

    private static PublicEndpointRateLimitFilter limited(AtomicLong now) {
        return new PublicEndpointRateLimitFilter(
            true,
            1,
            1,
            60,
            now::get
        );
    }

    private static int postWithHeader(
        PublicEndpointRateLimitFilter filter,
        String remoteAddress,
        String forwardedFor
    ) throws ServletException, IOException {

        MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(remoteAddress);
        request.addHeader("X-Forwarded-For", forwardedFor);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });
        return response.getStatus();
    }

    private static int postBehindProxy(
        ForwardedHeaderFilter forwardedHeaders,
        PublicEndpointRateLimitFilter rateLimit,
        String clientAddress
    ) throws ServletException, IOException {

        MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("X-Forwarded-For", clientAddress);

        MockHttpServletResponse response = new MockHttpServletResponse();

        forwardedHeaders.doFilter(request, response, (servletRequest, servletResponse) ->
            rateLimit.doFilter(
                (HttpServletRequest) servletRequest,
                (HttpServletResponse) servletResponse,
                (ignoredRequest, ignoredResponse) -> {
                }
            )
        );

        return response.getStatus();
    }

    private static int post(
        PublicEndpointRateLimitFilter filter,
        String path,
        String remoteAddress
    ) throws ServletException, IOException {

        return dispatch(
            filter,
            "POST",
            path,
            remoteAddress,
            new AtomicInteger()
        ).getStatus();
    }

    private static MockHttpServletResponse dispatch(
        PublicEndpointRateLimitFilter filter,
        String method,
        String path,
        String remoteAddress,
        AtomicInteger forwarded
    ) throws ServletException, IOException {

        MockHttpServletRequest request =
            new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddress);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
            request,
            response,
            (servletRequest, servletResponse) -> forwarded.incrementAndGet()
        );

        return response;
    }
}
