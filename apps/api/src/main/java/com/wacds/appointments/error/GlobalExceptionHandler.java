package com.wacds.appointments.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================
    // 404 · DOMAIN RESOURCE NOT FOUND
    // =========================================================

    /**
     * Recursos de dominio inexistentes.
     *
     * Ejemplos:
     *
     * - business inexistente
     * - professional inexistente
     * - service inexistente
     * - appointment inexistente
     * - customer inexistente
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse>
        handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            safeMessage(
                exception.getMessage(),
                "El recurso solicitado no existe."
            ),
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 404 · HTTP ROUTE NOT FOUND
    // =========================================================

    /**
     * Spring MVC puede enviar una ruta inexistente al
     * ResourceHttpRequestHandler.
     *
     * En ese caso lanza NoResourceFoundException.
     *
     * Antes esta excepción terminaba llegando al handler
     * genérico y se convertía incorrectamente en 500.
     *
     * Debe ser siempre:
     *
     * ruta inexistente -> 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse>
        handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
        ) {

        log.debug(
            "Resource not found processing {} {}",
            request.getMethod(),
            request.getRequestURI()
        );

        return buildResponse(
            HttpStatus.NOT_FOUND,
            "ENDPOINT_NOT_FOUND",
            "El endpoint solicitado no existe.",
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 409 · CONFLICT
    // =========================================================

    /**
     * Conflictos de negocio.
     *
     * Ejemplos:
     *
     * - reserva solapada
     * - recurso ya existente
     * - profesional no reservable
     * - servicio no reservable
     * - estado incompatible
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse>
        handleConflict(
            ConflictException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.CONFLICT,
            "CONFLICT",
            safeMessage(
                exception.getMessage(),
                "La operación entra en conflicto con el estado actual del recurso."
            ),
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 401 · BAD CREDENTIALS
    // =========================================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse>
        handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS",
            safeAuthenticationMessage(
                exception.getMessage()
            ),
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 401 · GENERIC AUTHENTICATION FAILURE
    // =========================================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse>
        handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            safeAuthenticationMessage(
                exception.getMessage()
            ),
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 403 · ACCESS DENIED
    // =========================================================

    /**
     * Usuario autenticado pero sin permisos suficientes.
     *
     * No devolvemos detalles internos sobre:
     *
     * - roles requeridos
     * - business propietario
     * - membresías existentes
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse>
        handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.FORBIDDEN,
            "ACCESS_DENIED",
            "No tienes permisos suficientes para realizar esta operación.",
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 400 · BEAN VALIDATION
    // =========================================================

    /**
     * Errores de @Valid sobre DTOs recibidos mediante
     * @RequestBody.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse>
        handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
        ) {

        Map<String, String> validationErrors =
            new LinkedHashMap<>();

        exception
            .getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                validationErrors.put(
                    error.getField(),
                    safeMessage(
                        error.getDefaultMessage(),
                        "Valor no válido."
                    )
                )
            );

        /*
         * También contemplamos errores globales del objeto,
         * no asociados directamente a un campo.
         */
        exception
            .getBindingResult()
            .getGlobalErrors()
            .forEach(error ->
                validationErrors.put(
                    error.getObjectName(),
                    safeMessage(
                        error.getDefaultMessage(),
                        "Objeto no válido."
                    )
                )
            );

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "La petición contiene campos no válidos.",
            request.getRequestURI(),
            validationErrors
        );
    }

    // =========================================================
    // 400 · CONSTRAINT VIOLATION
    // =========================================================

    /**
     * Validaciones Jakarta realizadas fuera del @RequestBody,
     * por ejemplo sobre parámetros o métodos validados.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse>
        handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
        ) {

        Map<String, String> validationErrors =
            new LinkedHashMap<>();

        for (
            ConstraintViolation<?> violation :
            exception.getConstraintViolations()
        ) {

            String field =
                violation.getPropertyPath() == null
                    ? "parameter"
                    : violation
                        .getPropertyPath()
                        .toString();

            validationErrors.put(
                field,
                safeMessage(
                    violation.getMessage(),
                    "Valor no válido."
                )
            );
        }

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "CONSTRAINT_VIOLATION",
            "La petición contiene parámetros no válidos.",
            request.getRequestURI(),
            validationErrors
        );
    }

    // =========================================================
    // 400 · MISSING REQUEST PARAMETER
    // =========================================================

    /**
     * Ejemplo:
     *
     * GET /availability-slots
     *
     * sin professionalId, serviceId o date.
     */
    @ExceptionHandler(
        MissingServletRequestParameterException.class
    )
    public ResponseEntity<ApiErrorResponse>
        handleMissingRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
        ) {

        Map<String, String> validationErrors =
            new LinkedHashMap<>();

        validationErrors.put(
            exception.getParameterName(),
            "El parámetro es obligatorio."
        );

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "MISSING_REQUEST_PARAMETER",
            "Falta un parámetro obligatorio en la petición.",
            request.getRequestURI(),
            validationErrors
        );
    }

    // =========================================================
    // 400 · ARGUMENT TYPE MISMATCH
    // =========================================================

    /**
     * Ejemplos:
     *
     * professionalId=abc
     * serviceId=test
     *
     * cuando Spring espera Long.
     */
    @ExceptionHandler(
        MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<ApiErrorResponse>
        handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
        ) {

        Map<String, String> validationErrors =
            new LinkedHashMap<>();

        String expectedType =
            exception.getRequiredType() == null
                ? "válido"
                : exception
                    .getRequiredType()
                    .getSimpleName();

        validationErrors.put(
            exception.getName(),
            "El valor debe tener un formato "
                + expectedType
                + "."
        );

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER_TYPE",
            "Uno de los parámetros tiene un formato no válido.",
            request.getRequestURI(),
            validationErrors
        );
    }

    // =========================================================
    // 400 · MALFORMED JSON
    // =========================================================

    /**
     * JSON inválido o tipos incompatibles en @RequestBody.
     *
     * No exponemos el mensaje interno de Jackson porque
     * podría revelar detalles innecesarios de implementación.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse>
        handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST_BODY",
            "El cuerpo de la petición no tiene un formato válido.",
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 400 · ILLEGAL ARGUMENT
    // =========================================================

    /**
     * Validaciones defensivas realizadas manualmente
     * dentro de los servicios.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse>
        handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            safeMessage(
                exception.getMessage(),
                "La petición contiene datos no válidos."
            ),
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 405 · METHOD NOT ALLOWED
    // =========================================================

    /**
     * La ruta existe pero no admite el método HTTP utilizado.
     *
     * Ejemplo:
     *
     * PUT sobre un endpoint que solo admite GET.
     */
    @ExceptionHandler(
        HttpRequestMethodNotSupportedException.class
    )
    public ResponseEntity<ApiErrorResponse>
        handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "El método HTTP utilizado no está permitido para este endpoint.",
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 415 · UNSUPPORTED MEDIA TYPE
    // =========================================================

    /**
     * Ejemplo:
     *
     * enviar text/plain a un endpoint que espera
     * application/json.
     */
    @ExceptionHandler(
        HttpMediaTypeNotSupportedException.class
    )
    public ResponseEntity<ApiErrorResponse>
        handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
        ) {

        return buildResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "UNSUPPORTED_MEDIA_TYPE",
            "El tipo de contenido enviado no está soportado.",
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // 500 · UNEXPECTED
    // =========================================================

    /**
     * Última barrera.
     *
     * Cualquier excepción no contemplada explícitamente
     * representa un fallo interno real.
     *
     * El stack trace se registra únicamente en servidor.
     * Nunca se devuelve al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse>
        handleUnexpected(
            Exception exception,
            HttpServletRequest request
        ) {

        log.error(
            "Unexpected exception processing {} {}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "Se ha producido un error interno inesperado.",
            request.getRequestURI(),
            null
        );
    }

    // =========================================================
    // RESPONSE BUILDER
    // =========================================================

    private ResponseEntity<ApiErrorResponse>
        buildResponse(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> validationErrors
        ) {

        ApiErrorResponse response =
            new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                safeMessage(
                    message,
                    status.getReasonPhrase()
                ),
                path,
                validationErrors
            );

        return ResponseEntity
            .status(
                status
            )
            .body(
                response
            );
    }

    // =========================================================
    // GENERIC MESSAGE SANITIZATION
    // =========================================================

    private String safeMessage(
        String message,
        String fallback
    ) {

        if (
            message == null
                || message.isBlank()
        ) {
            return fallback;
        }

        return message.trim();
    }

    // =========================================================
    // SECURITY MESSAGE SANITIZATION
    // =========================================================

    /**
     * Evita devolver null o mensajes vacíos durante
     * errores de autenticación.
     *
     * Los errores del filtro JWT siguen siendo responsabilidad
     * del AuthenticationEntryPoint configurado en SecurityConfig.
     */
    private String safeAuthenticationMessage(
        String message
    ) {

        if (
            message == null
                || message.isBlank()
        ) {
            return "No se ha podido autenticar la petición.";
        }

        return message.trim();
    }
}