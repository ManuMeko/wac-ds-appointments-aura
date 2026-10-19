package com.wacds.appointments.config;

import com.wacds.appointments.auth.security.CustomUserDetailsService;
import com.wacds.appointments.auth.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MINIMUM_HS256_SECRET_BYTES =
        32;

    private static final int BCRYPT_STRENGTH =
        12;

    private static final long CORS_MAX_AGE_SECONDS =
        3600L;

    // =========================================================
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(

        HttpSecurity http,

        JwtAuthenticationFilter jwtAuthenticationFilter,

        AuthenticationProvider authenticationProvider,

        CorsConfigurationSource corsConfigurationSource

    ) throws Exception {

        http

            // =================================================
            // CORS
            // =================================================

            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource
                )
            )

            // =================================================
            // STATELESS REST API
            // =================================================

            .csrf(
                AbstractHttpConfigurer::disable
            )

            .formLogin(
                AbstractHttpConfigurer::disable
            )

            .httpBasic(
                AbstractHttpConfigurer::disable
            )

            .logout(
                AbstractHttpConfigurer::disable
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // =================================================
            // USERNAME / PASSWORD AUTHENTICATION
            // =================================================

            .authenticationProvider(
                authenticationProvider
            )

            // =================================================
            // SECURITY EXCEPTION HANDLING
            // =================================================

            .exceptionHandling(exceptions -> exceptions

                // ---------------------------------------------
                // 401 · UNAUTHENTICATED
                // ---------------------------------------------

                .authenticationEntryPoint(
                    (request, response, exception) -> {

                        response.setStatus(
                            HttpServletResponse.SC_UNAUTHORIZED
                        );

                        response.setCharacterEncoding(
                            StandardCharsets.UTF_8.name()
                        );

                        response.setContentType(
                            MediaType.APPLICATION_JSON_VALUE
                        );

                        response
                            .getWriter()
                            .write(
                                """
                                {
                                  "status": 401,
                                  "error": "Unauthorized",
                                  "code": "UNAUTHORIZED",
                                  "message": "Es necesario autenticarse para acceder a este recurso."
                                }
                                """
                            );
                    }
                )

                // ---------------------------------------------
                // 403 · AUTHENTICATED BUT FORBIDDEN
                // ---------------------------------------------

                .accessDeniedHandler(
                    (request, response, exception) -> {

                        response.setStatus(
                            HttpServletResponse.SC_FORBIDDEN
                        );

                        response.setCharacterEncoding(
                            StandardCharsets.UTF_8.name()
                        );

                        response.setContentType(
                            MediaType.APPLICATION_JSON_VALUE
                        );

                        response
                            .getWriter()
                            .write(
                                """
                                {
                                  "status": 403,
                                  "error": "Forbidden",
                                  "code": "ACCESS_DENIED",
                                  "message": "No tienes permisos suficientes para acceder a este recurso."
                                }
                                """
                            );
                    }
                )
            )

            // =================================================
            // AUTHORIZATION
            // =================================================

            .authorizeHttpRequests(auth -> auth

                // ---------------------------------------------
                // CORS PREFLIGHT
                // ---------------------------------------------

                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                )
                .permitAll()

                // ---------------------------------------------
                // INFRASTRUCTURE · PUBLIC
                // ---------------------------------------------

                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/error"
                )
                .permitAll()

                // ---------------------------------------------
                // AUTH · PUBLIC
                // ---------------------------------------------

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh"
                )
                .permitAll()

                // ---------------------------------------------
                // PUBLIC AVAILABILITY
                // ---------------------------------------------

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/public/availability-slots"
                )
                .permitAll()

                // ---------------------------------------------
                // PUBLIC CATALOG
                // ---------------------------------------------

                /*
                 * Catálogo anónimo utilizado por /reservar.
                 *
                 * Únicamente permitimos GET.
                 *
                 * No abrimos globalmente:
                 *
                 * /api/v1/public/**
                 *
                 * para mantener protegidos por defecto futuros
                 * endpoints públicos que puedan requerir otro
                 * mecanismo de autorización.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/public/catalog/**"
                )
                .permitAll()

                // ---------------------------------------------
                // PUBLIC BOOKING · CREATE
                // ---------------------------------------------

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/public/bookings"
                )
                .permitAll()

                // ---------------------------------------------
                // PUBLIC BOOKING · MANAGEMENT VIEW
                // ---------------------------------------------

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/public/bookings/manage"
                )
                .permitAll()

                // ---------------------------------------------
                // PUBLIC BOOKING · CANCEL
                // ---------------------------------------------

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/public/bookings/manage/cancel"
                )
                .permitAll()

                // ---------------------------------------------
                // PUBLIC BOOKING · RESCHEDULE
                // ---------------------------------------------

                /*
                 * Reprogramación pública mediante:
                 *
                 * X-Booking-Management-Token
                 *
                 * La autorización real se ejecuta dentro de:
                 *
                 * AppointmentManagementTokenService
                 *
                 * y:
                 *
                 * PublicBookingManagementService.
                 *
                 * No se abre /api/v1/public/** globalmente.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/public/bookings/manage/reschedule"
                )
                .permitAll()

                // ---------------------------------------------
                // AUTHENTICATED SESSION
                // ---------------------------------------------

                .requestMatchers(
                    "/api/v1/auth/me",
                    "/api/v1/auth/logout"
                )
                .authenticated()

                // ---------------------------------------------
                // PRIVATE AVAILABILITY
                // ---------------------------------------------

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/availability-slots"
                )
                .authenticated()

                // ---------------------------------------------
                // TEAM / MEMBERSHIP MANAGEMENT
                // ---------------------------------------------

                /*
                 * Aquí únicamente exigimos autenticación.
                 *
                 * NO utilizamos hasRole(...) porque:
                 *
                 * - OWNER / ADMIN / STAFF pertenecen a una
                 *   BusinessMembership concreta;
                 *
                 * - el rol persistido en PostgreSQL manda
                 *   sobre cualquier claim antiguo del JWT;
                 *
                 * - BusinessMembershipService aplica las reglas
                 *   específicas de autorización.
                 */
                .requestMatchers(
                    "/api/v1/businesses/*/members",
                    "/api/v1/businesses/*/members/**"
                )
                .authenticated()

                // ---------------------------------------------
                // PRIVATE BUSINESS API
                // ---------------------------------------------

                /*
                 * Todo el resto de /api/v1 permanece protegido.
                 *
                 * La autorización específica continúa siendo
                 * responsabilidad de:
                 *
                 * - JwtAuthenticationFilter;
                 * - BusinessMembershipService;
                 * - servicios tenant-safe.
                 */
                .requestMatchers(
                    "/api/v1/**"
                )
                .authenticated()

                // ---------------------------------------------
                // FALLBACK
                // ---------------------------------------------

                .anyRequest()
                .authenticated()
            )

            // =================================================
            // JWT FILTER
            // =================================================

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // =========================================================
    // CORS
    // =========================================================

    /**
     * Configuración CORS central de la API.
     *
     * Desarrollo por defecto:
     *
     * - http://localhost:3000
     * - http://127.0.0.1:3000
     *
     * Producción:
     *
     * configurar:
     *
     * app.security.cors.allowed-origins
     *
     * con una lista separada por comas.
     *
     * Ejemplo:
     *
     * app.security.cors.allowed-origins=
     * https://panel.example.com,https://example.com
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(

        @Value(
            "${app.security.cors.allowed-origins:"
                + "http://localhost:3000,"
                + "http://127.0.0.1:3000}"
        )
        String allowedOriginsProperty

    ) {

        CorsConfiguration configuration =
            new CorsConfiguration();

        // -----------------------------------------------------
        // ALLOWED ORIGINS
        // -----------------------------------------------------

        List<String> allowedOrigins =
            Arrays.stream(
                    allowedOriginsProperty.split(",")
                )
                .map(
                    String::trim
                )
                .filter(
                    value ->
                        !value.isBlank()
                )
                .distinct()
                .toList();

        if (
            allowedOrigins.isEmpty()
        ) {

            throw new IllegalStateException(
                "app.security.cors.allowed-origins "
                    + "debe contener al menos un origen."
            );
        }

        configuration.setAllowedOrigins(
            allowedOrigins
        );

        // -----------------------------------------------------
        // HTTP METHODS
        // -----------------------------------------------------

        configuration.setAllowedMethods(
            List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
            )
        );

        // -----------------------------------------------------
        // REQUEST HEADERS
        // -----------------------------------------------------

        configuration.setAllowedHeaders(
            List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                HttpHeaders.ORIGIN,
                "X-Booking-Management-Token"
            )
        );

        // -----------------------------------------------------
        // RESPONSE HEADERS EXPOSED TO BROWSER
        // -----------------------------------------------------

        configuration.setExposedHeaders(
            List.of(
                HttpHeaders.LOCATION
            )
        );

        // -----------------------------------------------------
        // CREDENTIALS
        // -----------------------------------------------------

        /*
         * Actualmente la autenticación Business utiliza:
         *
         * Authorization: Bearer <JWT>
         *
         * y NO cookies de sesión.
         *
         * Por tanto no necesitamos credentials=true.
         *
         * Esto reduce superficie de ataque y evita combinar
         * credenciales con orígenes innecesariamente amplios.
         */
        configuration.setAllowCredentials(
            false
        );

        // -----------------------------------------------------
        // PREFLIGHT CACHE
        // -----------------------------------------------------

        configuration.setMaxAge(
            CORS_MAX_AGE_SECONDS
        );

        // -----------------------------------------------------
        // REGISTER
        // -----------------------------------------------------

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }

    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    /**
     * PasswordEncoder central de toda la aplicación.
     *
     * Se utiliza para:
     *
     * - login mediante DaoAuthenticationProvider;
     * - bootstrap de desarrollo;
     * - creación de miembros;
     * - cambios de contraseña.
     *
     * Nunca se almacenan contraseñas en texto plano.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(
            BCRYPT_STRENGTH
        );
    }

    // =========================================================
    // USERNAME / PASSWORD AUTHENTICATION PROVIDER
    // =========================================================

    @Bean
    public AuthenticationProvider authenticationProvider(

        CustomUserDetailsService customUserDetailsService,

        PasswordEncoder passwordEncoder

    ) {

        DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(
                customUserDetailsService
            );

        provider.setPasswordEncoder(
            passwordEncoder
        );

        return provider;
    }

    // =========================================================
    // AUTHENTICATION MANAGER
    // =========================================================

    @Bean
    public AuthenticationManager authenticationManager(

        AuthenticationProvider authenticationProvider

    ) {

        return new ProviderManager(
            authenticationProvider
        );
    }

    // =========================================================
    // JWT SECRET KEY
    // =========================================================

    /**
     * Construye la clave HMAC utilizada para HS256.
     *
     * Se exige:
     *
     * 32 bytes = 256 bits como mínimo.
     */
    @Bean
    public SecretKey jwtSecretKey(

        @Value("${app.security.jwt.secret}")
        String jwtSecret

    ) {

        if (
            jwtSecret == null
                || jwtSecret.isBlank()
        ) {

            throw new IllegalStateException(
                "app.security.jwt.secret es obligatorio."
            );
        }

        byte[] secretBytes =
            jwtSecret.getBytes(
                StandardCharsets.UTF_8
            );

        if (
            secretBytes.length
                < MINIMUM_HS256_SECRET_BYTES
        ) {

            throw new IllegalStateException(
                "app.security.jwt.secret debe contener "
                    + "como mínimo "
                    + MINIMUM_HS256_SECRET_BYTES
                    + " bytes."
            );
        }

        return new SecretKeySpec(
            secretBytes,
            "HmacSHA256"
        );
    }

    // =========================================================
    // JWT ENCODER
    // =========================================================

    @Bean
    public JwtEncoder jwtEncoder(

        SecretKey jwtSecretKey

    ) {

        return NimbusJwtEncoder
            .withSecretKey(
                jwtSecretKey
            )
            .algorithm(
                MacAlgorithm.HS256
            )
            .build();
    }

    // =========================================================
    // JWT DECODER
    // =========================================================

    /**
     * Configura la validación de:
     *
     * - firma;
     * - expiración;
     * - not-before;
     * - issuer.
     */
    @Bean
    public JwtDecoder jwtDecoder(

        SecretKey jwtSecretKey,

        @Value(
            "${app.security.jwt.issuer:wac-ds-appointments}"
        )
        String issuer

    ) {

        if (
            issuer == null
                || issuer.isBlank()
        ) {

            throw new IllegalStateException(
                "app.security.jwt.issuer es obligatorio."
            );
        }

        NimbusJwtDecoder decoder =
            NimbusJwtDecoder
                .withSecretKey(
                    jwtSecretKey
                )
                .macAlgorithm(
                    MacAlgorithm.HS256
                )
                .build();

        decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(
                issuer.trim()
            )
        );

        return decoder;
    }
}