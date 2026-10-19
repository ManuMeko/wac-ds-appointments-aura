package com.wacds.appointments.auth;

import com.wacds.appointments.auth.dto.CurrentUserResponse;
import com.wacds.appointments.auth.dto.LoginRequest;
import com.wacds.appointments.auth.dto.LoginResponse;
import com.wacds.appointments.auth.dto.RefreshTokenRequest;
import com.wacds.appointments.auth.dto.TokenResponse;
import com.wacds.appointments.auth.security.AuthenticatedUser;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final AuthService authService;

    // =========================================================
    // LOGIN
    // =========================================================

    /**
     * Autentica un usuario mediante email y contraseña.
     *
     * Devuelve:
     *
     * - access token JWT
     * - refresh token
     * - expiraciones
     * - información básica del usuario
     * - negocio activo
     * - rol dentro del negocio
     *
     * Endpoint público.
     *
     * La respuesta no debe almacenarse en caché porque contiene
     * credenciales de sesión.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(

        @Valid
        @RequestBody
        LoginRequest request

    ) {

        LoginResponse response =
            authService.login(
                request
            );

        return ResponseEntity
            .ok()
            .cacheControl(
                CacheControl.noStore()
            )
            .body(
                response
            );
    }

    // =========================================================
    // REFRESH
    // =========================================================

    /**
     * Renueva una sesión utilizando un refresh token.
     *
     * El refresh token utilizado se revoca y se emite uno nuevo.
     *
     * Esto implementa rotación de refresh tokens.
     *
     * Endpoint público porque el access token puede haber
     * expirado cuando el cliente necesita renovar la sesión.
     *
     * La respuesta contiene nuevas credenciales y, por tanto,
     * tampoco debe almacenarse en caché.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(

        @Valid
        @RequestBody
        RefreshTokenRequest request

    ) {

        TokenResponse response =
            authService.refresh(
                request
            );

        return ResponseEntity
            .ok()
            .cacheControl(
                CacheControl.noStore()
            )
            .body(
                response
            );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    /**
     * Cierra la sesión actual.
     *
     * Requiere:
     *
     * - access token válido en Authorization
     * - refresh token correspondiente en el body
     *
     * El refresh token queda revocado.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(

        @Valid
        @RequestBody
        RefreshTokenRequest request,

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        authService.logout(
            request,
            authenticatedUser
        );

        return ResponseEntity
            .status(
                HttpStatus.NO_CONTENT
            )
            .cacheControl(
                CacheControl.noStore()
            )
            .build();
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    /**
     * Devuelve la información del usuario correspondiente
     * a la sesión autenticada actual.
     *
     * Requiere access token JWT válido.
     *
     * Esta respuesta depende de la sesión y tampoco debe quedar
     * almacenada en caché.
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(

        @AuthenticationPrincipal
        AuthenticatedUser authenticatedUser

    ) {

        CurrentUserResponse response =
            authService.getCurrentUser(
                authenticatedUser
            );

        return ResponseEntity
            .ok()
            .cacheControl(
                CacheControl.noStore()
            )
            .body(
                response
            );
    }
}