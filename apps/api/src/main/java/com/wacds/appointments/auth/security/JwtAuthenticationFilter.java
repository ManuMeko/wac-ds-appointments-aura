package com.wacds.appointments.auth.security;

import com.wacds.appointments.auth.membership.BusinessMembership;
import com.wacds.appointments.auth.membership.BusinessMembershipRepository;
import com.wacds.appointments.auth.membership.BusinessRole;
import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.auth.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
    extends OncePerRequestFilter {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String BEARER_PREFIX =
        "Bearer ";

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final BusinessMembershipRepository
        businessMembershipRepository;

    // =========================================================
    // FILTER
    // =========================================================

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        /*
         * Si ya existe una autenticación válida en el contexto,
         * no volvemos a procesar el JWT.
         */
        if (
            SecurityContextHolder
                .getContext()
                .getAuthentication()
                != null
        ) {
            filterChain.doFilter(
                request,
                response
            );

            return;
        }

        String authorizationHeader =
            request.getHeader(
                HttpHeaders.AUTHORIZATION
            );

        /*
         * Las rutas públicas también atraviesan este filtro.
         *
         * Si no existe Bearer token simplemente continuamos.
         * Será SecurityConfig quien decida si el endpoint
         * requiere autenticación o es público.
         */
        if (
            authorizationHeader == null
                || authorizationHeader.isBlank()
                || !authorizationHeader.startsWith(
                    BEARER_PREFIX
                )
        ) {
            filterChain.doFilter(
                request,
                response
            );

            return;
        }

        String token =
            authorizationHeader
                .substring(
                    BEARER_PREFIX.length()
                )
                .trim();

        if (token.isBlank()) {
            unauthorized(
                response,
                "El access token está vacío."
            );

            return;
        }

        try {

            Jwt jwt =
                jwtService.decode(
                    token
                );

            validateJwtType(
                jwt
            );

            validateJwtExpiration(
                jwt
            );

            Long userId =
                jwtService.extractUserId(
                    jwt
                );

            String email =
                jwtService.extractEmail(
                    jwt
                );

            Long businessId =
                jwtService.extractBusinessId(
                    jwt
                );

            BusinessRole tokenRole =
                extractBusinessRole(
                    jwt
                );

            User user =
                userRepository
                    .findById(
                        userId
                    )
                    .orElseThrow(() ->
                        new JwtAuthenticationException(
                            "El usuario asociado al token no existe."
                        )
                    );

            validateUser(
                user,
                email
            );

            BusinessMembership membership =
                businessMembershipRepository
                    .findByBusinessIdAndUserIdAndActiveTrue(
                        businessId,
                        userId
                    )
                    .orElseThrow(() ->
                        new JwtAuthenticationException(
                            "El usuario ya no tiene acceso activo al negocio indicado."
                        )
                    );

            validateMembership(
                membership,
                tokenRole
            );

            AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                    user.getId(),
                    user.getEmail(),
                    membership
                        .getBusiness()
                        .getId(),
                    membership.getRole()
                );

            List<GrantedAuthority> authorities =
                List.of(
                    new SimpleGrantedAuthority(
                        "ROLE_USER"
                    ),
                    new SimpleGrantedAuthority(
                        "ROLE_"
                            + membership
                                .getRole()
                                .name()
                    )
                );

            UsernamePasswordAuthenticationToken
                authentication =
                    new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authorities
                    );

            SecurityContextHolder
                .getContext()
                .setAuthentication(
                    authentication
                );

            filterChain.doFilter(
                request,
                response
            );

        } catch (JwtException exception) {

            SecurityContextHolder
                .clearContext();

            unauthorized(
                response,
                "El access token no es válido o ha expirado."
            );

        } catch (
            IllegalArgumentException
                | JwtAuthenticationException exception
        ) {

            SecurityContextHolder
                .clearContext();

            unauthorized(
                response,
                exception.getMessage()
            );
        }
    }

    // =========================================================
    // JWT VALIDATION
    // =========================================================

    private void validateJwtType(
        Jwt jwt
    ) {
        if (
            !jwtService.isAccessToken(
                jwt
            )
        ) {
            throw new JwtAuthenticationException(
                "El token indicado no es un access token."
            );
        }
    }

    private void validateJwtExpiration(
        Jwt jwt
    ) {
        if (
            jwtService.isExpired(
                jwt
            )
        ) {
            throw new JwtAuthenticationException(
                "El access token ha expirado."
            );
        }
    }

    private BusinessRole extractBusinessRole(
        Jwt jwt
    ) {
        String role =
            jwtService.extractRole(
                jwt
            );

        try {
            return BusinessRole.valueOf(
                role
            );
        } catch (IllegalArgumentException exception) {
            throw new JwtAuthenticationException(
                "El JWT contiene un rol de negocio no válido."
            );
        }
    }

    // =========================================================
    // USER VALIDATION
    // =========================================================

    private void validateUser(
        User user,
        String tokenEmail
    ) {
        if (!user.isActive()) {
            throw new JwtAuthenticationException(
                "La cuenta de usuario está desactivada."
            );
        }

        if (
            user.getEmail() == null
                || !user
                    .getEmail()
                    .equalsIgnoreCase(
                        tokenEmail
                    )
        ) {
            throw new JwtAuthenticationException(
                "Los datos del access token ya no coinciden "
                    + "con los datos actuales del usuario."
            );
        }
    }

    // =========================================================
    // MEMBERSHIP VALIDATION
    // =========================================================

    private void validateMembership(
        BusinessMembership membership,
        BusinessRole tokenRole
    ) {
        if (!membership.isActive()) {
            throw new JwtAuthenticationException(
                "La membresía del usuario está desactivada."
            );
        }

        if (
            membership.getBusiness() == null
                || membership
                    .getBusiness()
                    .getId()
                    == null
        ) {
            throw new JwtAuthenticationException(
                "La membresía no tiene un negocio válido asociado."
            );
        }

        if (
            !membership
                .getBusiness()
                .isActive()
        ) {
            throw new JwtAuthenticationException(
                "El negocio asociado a la sesión está desactivado."
            );
        }

        if (
            membership.getRole() == null
        ) {
            throw new JwtAuthenticationException(
                "La membresía no tiene un rol válido."
            );
        }

        /*
         * Si el administrador cambia el rol de un usuario,
         * los JWT antiguos dejan de ser válidos inmediatamente.
         */
        if (
            membership.getRole()
                != tokenRole
        ) {
            throw new JwtAuthenticationException(
                "El rol del usuario ha cambiado. "
                    + "Es necesario iniciar sesión nuevamente."
            );
        }
    }

    // =========================================================
    // UNAUTHORIZED RESPONSE
    // =========================================================

    private void unauthorized(
        HttpServletResponse response,
        String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.resetBuffer();

        response.setStatus(
            HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
        );

        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );

        String safeMessage =
            sanitizeJsonMessage(
                message
            );

        response
            .getWriter()
            .write(
                """
                {
                  "status": 401,
                  "error": "Unauthorized",
                  "code": "UNAUTHORIZED",
                  "message": "%s"
                }
                """.formatted(
                    safeMessage
                )
            );

        response.flushBuffer();
    }

    private String sanitizeJsonMessage(
        String message
    ) {
        if (
            message == null
                || message.isBlank()
        ) {
            return "No se ha podido autenticar la petición.";
        }

        return message
            .replace(
                "\\",
                "\\\\"
            )
            .replace(
                "\"",
                "\\\""
            )
            .replace(
                "\r",
                ""
            )
            .replace(
                "\n",
                " "
            );
    }

    // =========================================================
    // INTERNAL EXCEPTION
    // =========================================================

    private static class JwtAuthenticationException
        extends RuntimeException {

        private JwtAuthenticationException(
            String message
        ) {
            super(
                message
            );
        }
    }
}