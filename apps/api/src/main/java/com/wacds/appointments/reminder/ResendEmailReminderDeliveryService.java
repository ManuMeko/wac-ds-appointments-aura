package com.wacds.appointments.reminder;

import com.wacds.appointments.reminder.AppointmentReminder.ReminderChannel;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proveedor real de EMAIL mediante Resend.
 *
 * =========================================================
 * RESPONSABILIDAD
 * =========================================================
 *
 * Este componente tiene dos usos:
 *
 * 1. entrega de AppointmentReminder;
 *
 * 2. entrega de emails transaccionales internos
 *    (por ejemplo confirmación de reserva).
 *
 * Toda la configuración HTTP / Resend queda centralizada aquí
 * para evitar duplicar:
 *
 * - API key;
 * - URL;
 * - remitente;
 * - reply-to;
 * - timeouts;
 * - serialización JSON;
 * - tratamiento de errores;
 * - extracción del providerMessageId.
 *
 * =========================================================
 * UTF-8 / ENCODING
 * =========================================================
 *
 * Los payloads hacia Resend se envían con:
 *
 * Content-Type:
 *
 * application/json; charset=UTF-8
 *
 * Además, las cadenas JSON se serializan de forma ASCII-safe.
 *
 * Los caracteres Unicode no ASCII se representan como:
 *
 * uXXXX
 *
 * Esto evita problemas intermedios de codificación con:
 *
 * - asuntos;
 * - nombres;
 * - acentos;
 * - eñes;
 * - contenido HTML.
 *
 * También se aplica una reparación defensiva de mojibake
 * únicamente cuando detectamos patrones típicos como:
 *
 * estÃ¡
 * confirmaciÃ³n
 *
 * que corresponden normalmente a UTF-8 interpretado
 * accidentalmente como ISO-8859-1.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * La API key:
 *
 * - nunca se persiste;
 * - nunca se devuelve;
 * - nunca se escribe directamente en logs;
 * - se redacta de cualquier excepción.
 *
 * Los callers de sendTransactionalEmail(...) tampoco deben
 * incluir secretos dentro de:
 *
 * - subject;
 * - idempotencyKey;
 * - logs.
 *
 * En particular, un managementToken de reserva únicamente
 * puede formar parte del HTML privado enviado al destinatario.
 *
 * Nunca:
 *
 * - de la idempotency key;
 * - de logs;
 * - de mensajes de error.
 */
@Slf4j
@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
    prefix = "app.reminders.email",
    name = "real-delivery-enabled",
    havingValue = "true"
)
public class ResendEmailReminderDeliveryService
    implements ReminderDeliveryService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String EMAIL_ENDPOINT =
        "/emails";

    private static final String AUTHORIZATION_HEADER =
        "Authorization";

    private static final String CONTENT_TYPE_HEADER =
        "Content-Type";

    private static final String ACCEPT_HEADER =
        "Accept";

    private static final String IDEMPOTENCY_KEY_HEADER =
        "Idempotency-Key";

    private static final String JSON_CONTENT_TYPE =
        "application/json; charset=UTF-8";

    private static final String JSON_ACCEPT =
        "application/json";

    private static final String BEARER_PREFIX =
        "Bearer ";

    private static final int MAX_SAFE_ERROR_LENGTH =
        500;

    private static final int MAX_RECIPIENT_LENGTH =
        320;

    private static final int MAX_SUBJECT_LENGTH =
        250;

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH =
        256;

    private static final int MAX_MOJIBAKE_REPAIR_PASSES =
        2;

    private static final Pattern JSON_ID_PATTERN =
        Pattern.compile(
            "\"id\"\\s*:\\s*\"([^\"]+)\""
        );

    private static final Pattern JSON_MESSAGE_PATTERN =
        Pattern.compile(
            "\"message\"\\s*:\\s*\"([^\"]+)\""
        );

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy 'a las' HH:mm",
            new Locale(
                "es",
                "ES"
            )
        );

    // =========================================================
    // CONFIGURATION
    // =========================================================

    private final String apiKey;

    private final String baseUrl;

    private final String fromEmail;

    private final String fromName;

    private final String replyTo;

    private final String reminderSubjectPrefix;

    private final int readTimeoutSeconds;

    // =========================================================
    // HTTP CLIENT
    // =========================================================

    private final HttpClient httpClient;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ResendEmailReminderDeliveryService(

        @Value("${app.resend.api-key:}")
        String apiKey,

        @Value("${app.resend.base-url:https://api.resend.com}")
        String baseUrl,

        @Value("${app.resend.from-email:onboarding@resend.dev}")
        String fromEmail,

        @Value("${app.resend.from-name:WAC D.S. Appointments}")
        String fromName,

        @Value("${app.resend.reply-to:}")
        String replyTo,

        @Value("${app.resend.reminder-subject-prefix:Recordatorio de tu cita}")
        String reminderSubjectPrefix,

        @Value("${app.resend.connect-timeout-seconds:5}")
        int connectTimeoutSeconds,

        @Value("${app.resend.read-timeout-seconds:15}")
        int readTimeoutSeconds

    ) {

        this.apiKey =
            normalizeNullable(
                apiKey
            );

        this.baseUrl =
            normalizeBaseUrl(
                baseUrl
            );

        this.fromEmail =
            normalizeNullable(
                fromEmail
            );

        this.fromName =
            normalizeNullable(
                repairCommonUtf8Mojibake(
                    fromName
                )
            );

        this.replyTo =
            normalizeNullable(
                replyTo
            );

        this.reminderSubjectPrefix =
            normalizeNullable(
                repairCommonUtf8Mojibake(
                    reminderSubjectPrefix
                )
            );

        this.readTimeoutSeconds =
            Math.max(
                readTimeoutSeconds,
                1
            );

        this.httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(
                    Duration.ofSeconds(
                        Math.max(
                            connectTimeoutSeconds,
                            1
                        )
                    )
                )
                .build();
    }

    // =========================================================
    // REMINDER SUPPORT
    // =========================================================

    @Override
    public boolean supports(
        ReminderChannel channel
    ) {

        return channel
            == ReminderChannel.EMAIL;
    }

    // =========================================================
    // REMINDER DELIVERY
    // =========================================================

    /**
     * Entrega un AppointmentReminder mediante Resend.
     *
     * El worker sigue siendo responsable de persistir:
     *
     * - SENT;
     * - FAILED;
     * - providerMessageId;
     * - intentos;
     * - errores.
     */
    @Override
    public DeliveryResult deliver(
        AppointmentReminder reminder
    ) {

        try {

            validateReminder(
                reminder
            );

            String recipient =
                reminder
                    .getDestination()
                    .trim();

            String subject =
                buildReminderSubject(
                    reminder
                );

            String html =
                buildReminderHtml(
                    reminder
                );

            String idempotencyKey =
                buildReminderIdempotencyKey(
                    reminder
                );

            return sendTransactionalEmail(
                recipient,
                subject,
                html,
                idempotencyKey
            );

        } catch (
            Exception exception
        ) {

            String error =
                safeExceptionMessage(
                    exception
                );

            log.warn(
                "Resend reminder delivery failed. "
                    + "reminderId={}, appointmentStartAt={}, error={}",
                safeReminderId(
                    reminder
                ),
                safeAppointmentStartAt(
                    reminder
                ),
                error
            );

            return DeliveryResult.failed(
                error
            );
        }
    }

    // =========================================================
    // GENERIC TRANSACTIONAL EMAIL
    // =========================================================

    /**
     * Envía un email HTML transaccional utilizando la misma
     * infraestructura Resend que los recordatorios.
     *
     * Este método está pensado para:
     *
     * - confirmación de reserva;
     * - gestión de reserva;
     * - futuras notificaciones operativas.
     *
     * IMPORTANTE:
     *
     * idempotencyKey debe ser estable entre reintentos.
     *
     * Nunca debe contener:
     *
     * - managementToken;
     * - API keys;
     * - JWT;
     * - passwords;
     * - otros secretos.
     */
    public DeliveryResult sendTransactionalEmail(
        String recipient,
        String subject,
        String html,
        String idempotencyKey
    ) {

        String normalizedIdempotencyKey =
            null;

        try {

            validateConfiguration();

            String normalizedRecipient =
                validateAndNormalizeRecipient(
                    recipient
                );

            String normalizedSubject =
                validateAndNormalizeSubject(
                    subject
                );

            String normalizedHtml =
                validateAndNormalizeHtml(
                    html
                );

            normalizedIdempotencyKey =
                validateAndNormalizeIdempotencyKey(
                    idempotencyKey
                );

            String requestBody =
                buildEmailRequestBody(
                    normalizedRecipient,
                    normalizedSubject,
                    normalizedHtml
                );

            URI endpoint =
                URI.create(
                    baseUrl
                        + EMAIL_ENDPOINT
                );

            HttpRequest request =
                HttpRequest
                    .newBuilder()
                    .uri(
                        endpoint
                    )
                    .timeout(
                        Duration.ofSeconds(
                            readTimeoutSeconds
                        )
                    )
                    .header(
                        AUTHORIZATION_HEADER,
                        BEARER_PREFIX
                            + apiKey
                    )
                    .header(
                        CONTENT_TYPE_HEADER,
                        JSON_CONTENT_TYPE
                    )
                    .header(
                        ACCEPT_HEADER,
                        JSON_ACCEPT
                    )
                    .header(
                        IDEMPOTENCY_KEY_HEADER,
                        normalizedIdempotencyKey
                    )
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            requestBody,
                            StandardCharsets.UTF_8
                        )
                    )
                    .build();

            HttpResponse<String> response =
                httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                    )
                );

            return handleResponse(
                normalizedIdempotencyKey,
                response
            );

        } catch (
            InterruptedException exception
        ) {

            Thread
                .currentThread()
                .interrupt();

            log.warn(
                "Resend transactional email interrupted. "
                    + "idempotencyKey={}",
                safeIdempotencyKey(
                    normalizedIdempotencyKey
                )
            );

            return DeliveryResult.failed(
                "El envío mediante Resend fue interrumpido."
            );

        } catch (
            Exception exception
        ) {

            String error =
                safeExceptionMessage(
                    exception
                );

            log.warn(
                "Resend transactional email failed. "
                    + "idempotencyKey={}, error={}",
                safeIdempotencyKey(
                    normalizedIdempotencyKey
                ),
                error
            );

            return DeliveryResult.failed(
                error
            );
        }
    }

    // =========================================================
    // CONFIGURATION VALIDATION
    // =========================================================

    private void validateConfiguration() {

        if (
            apiKey == null
        ) {

            throw new IllegalStateException(
                "RESEND_API_KEY no está configurada."
            );
        }

        if (
            !apiKey.startsWith(
                "re_"
            )
        ) {

            throw new IllegalStateException(
                "RESEND_API_KEY no tiene un formato válido."
            );
        }

        if (
            baseUrl == null
        ) {

            throw new IllegalStateException(
                "La URL base de Resend no está configurada."
            );
        }

        if (
            fromEmail == null
        ) {

            throw new IllegalStateException(
                "El remitente de Resend no está configurado."
            );
        }
    }

    // =========================================================
    // REMINDER VALIDATION
    // =========================================================

    private void validateReminder(
        AppointmentReminder reminder
    ) {

        if (
            reminder == null
        ) {

            throw new IllegalArgumentException(
                "AppointmentReminder es obligatorio."
            );
        }

        if (
            reminder.getId() == null
                || reminder.getId() <= 0
        ) {

            throw new IllegalArgumentException(
                "El reminder no tiene un id válido."
            );
        }

        if (
            reminder.getChannel()
                != ReminderChannel.EMAIL
        ) {

            throw new IllegalArgumentException(
                "ResendEmailReminderDeliveryService "
                    + "solo soporta EMAIL."
            );
        }

        if (
            !reminder.isProcessing()
        ) {

            throw new IllegalStateException(
                "El reminder debe estar en PROCESSING "
                    + "antes de llamar a Resend."
            );
        }

        if (
            reminder.getAppointment() == null
                || reminder
                    .getAppointment()
                    .getId() == null
        ) {

            throw new IllegalArgumentException(
                "El reminder no tiene una Appointment válida."
            );
        }

        if (
            reminder.getAppointmentStartAt()
                == null
        ) {

            throw new IllegalArgumentException(
                "El reminder no contiene appointmentStartAt."
            );
        }

        if (
            reminder.getReminderType()
                == null
        ) {

            throw new IllegalArgumentException(
                "El reminder no contiene reminderType."
            );
        }

        if (
            reminder.getDestination()
                == null
                || reminder
                    .getDestination()
                    .isBlank()
        ) {

            throw new IllegalArgumentException(
                "El reminder no tiene destino de email."
            );
        }
    }

    // =========================================================
    // TRANSACTIONAL EMAIL VALIDATION
    // =========================================================

    private String validateAndNormalizeRecipient(
        String recipient
    ) {

        String normalized =
            normalizeNullable(
                recipient
            );

        if (
            normalized == null
        ) {

            throw new IllegalArgumentException(
                "El destinatario del email es obligatorio."
            );
        }

        if (
            normalized.length()
                > MAX_RECIPIENT_LENGTH
        ) {

            throw new IllegalArgumentException(
                "El destinatario del email es demasiado largo."
            );
        }

        if (
            !normalized.contains(
                "@"
            )
        ) {

            throw new IllegalArgumentException(
                "El destinatario del email no tiene un formato válido."
            );
        }

        return normalized;
    }

    private String validateAndNormalizeSubject(
        String subject
    ) {

        String normalized =
            normalizeNullable(
                repairCommonUtf8Mojibake(
                    subject
                )
            );

        if (
            normalized == null
        ) {

            throw new IllegalArgumentException(
                "El asunto del email es obligatorio."
            );
        }

        if (
            normalized.length()
                > MAX_SUBJECT_LENGTH
        ) {

            throw new IllegalArgumentException(
                "El asunto del email es demasiado largo."
            );
        }

        return normalized;
    }

    private String validateAndNormalizeHtml(
        String html
    ) {

        String normalized =
            normalizeNullable(
                html
            );

        if (
            normalized == null
        ) {

            throw new IllegalArgumentException(
                "El contenido HTML del email es obligatorio."
            );
        }

        return normalized;
    }

    private String validateAndNormalizeIdempotencyKey(
        String idempotencyKey
    ) {

        String normalized =
            normalizeNullable(
                idempotencyKey
            );

        if (
            normalized == null
        ) {

            throw new IllegalArgumentException(
                "La idempotencyKey del email es obligatoria."
            );
        }

        if (
            normalized.length()
                > MAX_IDEMPOTENCY_KEY_LENGTH
        ) {

            throw new IllegalArgumentException(
                "La idempotencyKey del email es demasiado larga."
            );
        }

        if (
            containsUnsafeWhitespace(
                normalized
            )
        ) {

            throw new IllegalArgumentException(
                "La idempotencyKey contiene caracteres no permitidos."
            );
        }

        return normalized;
    }

    private boolean containsUnsafeWhitespace(
        String value
    ) {

        if (
            value == null
        ) {

            return false;
        }

        for (
            int index = 0;
            index < value.length();
            index++
        ) {

            char character =
                value.charAt(
                    index
                );

            if (
                character == '\r'
                    || character == '\n'
                    || character == '\t'
            ) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // GENERIC REQUEST BODY
    // =========================================================

    private String buildEmailRequestBody(
        String recipient,
        String subject,
        String html
    ) {

        StringBuilder json =
            new StringBuilder();

        json.append(
            "{"
        );

        json.append(
            "\"from\":\""
        );

        json.append(
            escapeJson(
                buildFrom()
            )
        );

        json.append(
            "\","
        );

        json.append(
            "\"to\":[\""
        );

        json.append(
            escapeJson(
                recipient
            )
        );

        json.append(
            "\"],"
        );

        json.append(
            "\"subject\":\""
        );

        json.append(
            escapeJson(
                subject
            )
        );

        json.append(
            "\","
        );

        json.append(
            "\"html\":\""
        );

        json.append(
            escapeJson(
                html
            )
        );

        json.append(
            "\""
        );

        if (
            replyTo != null
        ) {

            json.append(
                ",\"reply_to\":\""
            );

            json.append(
                escapeJson(
                    replyTo
                )
            );

            json.append(
                "\""
            );
        }

        json.append(
            "}"
        );

        return json.toString();
    }

    // =========================================================
    // FROM
    // =========================================================

    private String buildFrom() {

        if (
            fromName == null
        ) {

            return fromEmail;
        }

        return fromName
            + " <"
            + fromEmail
            + ">";
    }

    // =========================================================
    // REMINDER SUBJECT
    // =========================================================

    private String buildReminderSubject(
        AppointmentReminder reminder
    ) {

        String prefix =
            reminderSubjectPrefix != null
                ? reminderSubjectPrefix
                : "Recordatorio de tu cita";

        return prefix
            + " · "
            + formatAppointmentDateTime(
                reminder.getAppointmentStartAt()
            );
    }

    // =========================================================
    // REMINDER HTML
    // =========================================================

    private String buildReminderHtml(
        AppointmentReminder reminder
    ) {

        String formattedDate =
            escapeHtml(
                formatAppointmentDateTime(
                    reminder.getAppointmentStartAt()
                )
            );

        String reminderLabel =
            reminder.getReminderType() != null
                ? escapeHtml(
                    reminder
                        .getReminderType()
                        .name()
                )
                : "REMINDER";

        return """
            <!doctype html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta
                    name="viewport"
                    content="width=device-width, initial-scale=1.0"
                >
                <title>Recordatorio de cita</title>
            </head>

            <body style="
                margin:0;
                padding:0;
                background:#f6f3ef;
                font-family:Arial,Helvetica,sans-serif;
                color:#2f2925;
            ">

                <table
                    role="presentation"
                    width="100%%"
                    cellspacing="0"
                    cellpadding="0"
                    border="0"
                    style="
                        background:#f6f3ef;
                        padding:32px 16px;
                    "
                >
                    <tr>
                        <td align="center">

                            <table
                                role="presentation"
                                width="100%%"
                                cellspacing="0"
                                cellpadding="0"
                                border="0"
                                style="
                                    max-width:620px;
                                    background:#ffffff;
                                    border-radius:18px;
                                    overflow:hidden;
                                    border:1px solid #ebe5de;
                                "
                            >

                                <tr>
                                    <td style="
                                        padding:34px 36px 16px 36px;
                                    ">

                                        <div style="
                                            font-size:12px;
                                            letter-spacing:2px;
                                            text-transform:uppercase;
                                            color:#88786c;
                                            margin-bottom:14px;
                                        ">
                                            WAC D.S. Appointments
                                        </div>

                                        <h1 style="
                                            margin:0;
                                            font-size:28px;
                                            line-height:1.25;
                                            font-weight:600;
                                            color:#2f2925;
                                        ">
                                            Recordatorio de tu cita
                                        </h1>

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:12px 36px 4px 36px;
                                        font-size:16px;
                                        line-height:1.7;
                                        color:#554a43;
                                    ">
                                        Te recordamos que tienes una cita
                                        programada para:
                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:18px 36px;
                                    ">

                                        <div style="
                                            padding:22px;
                                            background:#f8f5f1;
                                            border-radius:14px;
                                            border:1px solid #ece5de;
                                        ">

                                            <div style="
                                                font-size:13px;
                                                color:#8a796e;
                                                margin-bottom:8px;
                                            ">
                                                Fecha y hora
                                            </div>

                                            <div style="
                                                font-size:21px;
                                                font-weight:600;
                                                color:#352e29;
                                            ">
                                                %s
                                            </div>

                                        </div>

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:8px 36px 30px 36px;
                                        font-size:14px;
                                        line-height:1.65;
                                        color:#74675e;
                                    ">

                                        Si ya has modificado o cancelado
                                        tu reserva, puedes ignorar este
                                        mensaje.

                                        <div style="
                                            margin-top:22px;
                                            font-size:12px;
                                            color:#a1948b;
                                        ">
                                            Tipo de recordatorio: %s
                                        </div>

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:22px 36px;
                                        background:#2f2925;
                                        color:#f5efe9;
                                        font-size:12px;
                                        line-height:1.6;
                                    ">
                                        Este mensaje ha sido generado
                                        automáticamente por el sistema
                                        de reservas.
                                    </td>
                                </tr>

                            </table>

                        </td>
                    </tr>
                </table>

            </body>
            </html>
            """
            .formatted(
                formattedDate,
                reminderLabel
            );
    }

    // =========================================================
    // REMINDER IDEMPOTENCY
    // =========================================================

    private String buildReminderIdempotencyKey(
        AppointmentReminder reminder
    ) {

        long appointmentEpochSecond =
            reminder
                .getAppointmentStartAt()
                .toEpochSecond();

        return "appointment-reminder/"
            + reminder.getId()
            + "/"
            + reminder.getReminderType().name()
            + "/"
            + appointmentEpochSecond;
    }

    // =========================================================
    // RESEND RESPONSE
    // =========================================================

    private DeliveryResult handleResponse(
        String safeContext,
        HttpResponse<String> response
    ) {

        if (
            response == null
        ) {

            return DeliveryResult.failed(
                "Resend no devolvió respuesta."
            );
        }

        int statusCode =
            response.statusCode();

        String body =
            response.body();

        if (
            statusCode >= 200
                && statusCode < 300
        ) {

            String providerMessageId =
                extractJsonValue(
                    body,
                    JSON_ID_PATTERN
                );

            log.info(
                "Resend accepted email. "
                    + "context={}, providerMessageId={}",
                safeIdempotencyKey(
                    safeContext
                ),
                providerMessageId
            );

            return DeliveryResult.sent(
                providerMessageId
            );
        }

        String providerMessage =
            extractJsonValue(
                body,
                JSON_MESSAGE_PATTERN
            );

        String providerError =
            providerMessage != null
                ? "Resend HTTP "
                    + statusCode
                    + ": "
                    + providerMessage
                : "Resend rechazó el envío con HTTP "
                    + statusCode
                    + ".";

        providerError =
            truncate(
                providerError,
                MAX_SAFE_ERROR_LENGTH
            );

        log.warn(
            "Resend rejected email. "
                + "context={}, statusCode={}, error={}",
            safeIdempotencyKey(
                safeContext
            ),
            statusCode,
            providerError
        );

        return DeliveryResult.failed(
            providerError
        );
    }

    // =========================================================
    // JSON EXTRACTION
    // =========================================================

    private String extractJsonValue(
        String body,
        Pattern pattern
    ) {

        if (
            body == null
                || body.isBlank()
                || pattern == null
        ) {

            return null;
        }

        Matcher matcher =
            pattern.matcher(
                body
            );

        if (
            !matcher.find()
        ) {

            return null;
        }

        return normalizeNullable(
            unescapeBasicJson(
                matcher.group(
                    1
                )
            )
        );
    }

    // =========================================================
    // DATE
    // =========================================================

    private String formatAppointmentDateTime(
        OffsetDateTime appointmentStartAt
    ) {

        if (
            appointmentStartAt == null
        ) {

            return "fecha pendiente";
        }

        return DATE_TIME_FORMATTER
            .format(
                appointmentStartAt
            );
    }

    // =========================================================
    // JSON SAFETY
    // =========================================================

    /**
     * Serializa una cadena para introducirla dentro de JSON.
     *
     * Además de escapar los caracteres de control, cualquier
     * carácter fuera del ASCII imprimible se convierte a
     * secuencia Unicode JSON.
     *
     * Ejemplo:
     *
     * á -> \u00e1
     *
     * De esta forma el payload HTTP final queda formado
     * exclusivamente por caracteres ASCII y evitamos
     * reinterpretaciones incorrectas de UTF-8.
     */
    private String escapeJson(
        String value
    ) {

        if (
            value == null
        ) {

            return "";
        }

        StringBuilder escaped =
            new StringBuilder();

        for (
            int index = 0;
            index < value.length();
            index++
        ) {

            char character =
                value.charAt(
                    index
                );

            switch (
                character
            ) {

                case '"' ->
                    escaped.append(
                        "\\\""
                    );

                case '\\' ->
                    escaped.append(
                        "\\\\"
                    );

                case '\b' ->
                    escaped.append(
                        "\\b"
                    );

                case '\f' ->
                    escaped.append(
                        "\\f"
                    );

                case '\n' ->
                    escaped.append(
                        "\\n"
                    );

                case '\r' ->
                    escaped.append(
                        "\\r"
                    );

                case '\t' ->
                    escaped.append(
                        "\\t"
                    );

                default -> {

                    if (
                        character < 0x20
                            || character > 0x7E
                    ) {

                        escaped.append(
                            String.format(
                                Locale.ROOT,
                                "\\u%04x",
                                (int) character
                            )
                        );

                    } else {

                        escaped.append(
                            character
                        );
                    }
                }
            }
        }

        return escaped.toString();
    }

    private String unescapeBasicJson(
        String value
    ) {

        if (
            value == null
        ) {

            return null;
        }

        return value
            .replace(
                "\\\"",
                "\""
            )
            .replace(
                "\\\\",
                "\\"
            )
            .replace(
                "\\n",
                "\n"
            )
            .replace(
                "\\r",
                "\r"
            )
            .replace(
                "\\t",
                "\t"
            );
    }

    // =========================================================
    // UTF-8 / MOJIBAKE SAFETY
    // =========================================================

    /**
     * Corrige defensivamente cadenas que contienen signos
     * característicos de UTF-8 interpretado como ISO-8859-1.
     *
     * Ejemplos:
     *
     * estÃ¡       -> está
     * confirmaciÃ³n -> confirmación
     *
     * IMPORTANTE:
     *
     * Solo se intenta reparar cuando existen patrones
     * reconocibles de mojibake.
     *
     * Una cadena Unicode correcta:
     *
     * "Tu reserva está confirmada"
     *
     * se devuelve intacta.
     */
    private String repairCommonUtf8Mojibake(
        String value
    ) {

        if (
            value == null
                || value.isEmpty()
        ) {

            return value;
        }

        String current =
            value;

        for (
            int pass = 0;
            pass < MAX_MOJIBAKE_REPAIR_PASSES;
            pass++
        ) {

            if (
                !looksLikeUtf8Mojibake(
                    current
                )
            ) {

                break;
            }

            int currentScore =
                mojibakeScore(
                    current
                );

            String repaired;

            try {

                byte[] bytes =
                    current.getBytes(
                        StandardCharsets.ISO_8859_1
                    );

                repaired =
                    new String(
                        bytes,
                        StandardCharsets.UTF_8
                    );

            } catch (
                Exception exception
            ) {

                break;
            }

            if (
                repaired.indexOf(
                    '\uFFFD'
                ) >= 0
            ) {

                break;
            }

            int repairedScore =
                mojibakeScore(
                    repaired
                );

            if (
                repairedScore >=
                    currentScore
            ) {

                break;
            }

            current =
                repaired;
        }

        return current;
    }

    private boolean looksLikeUtf8Mojibake(
        String value
    ) {

        return mojibakeScore(
            value
        ) > 0;
    }

    private int mojibakeScore(
        String value
    ) {

        if (
            value == null
                || value.isEmpty()
        ) {

            return 0;
        }

        int score =
            0;

        score +=
            countOccurrences(
                value,
                "Ã"
            );

        score +=
            countOccurrences(
                value,
                "Â"
            );

        score +=
            countOccurrences(
                value,
                "â"
            );

        score +=
            countOccurrences(
                value,
                "ð"
            );

        score +=
            countOccurrences(
                value,
                "ï¿½"
            );

        score +=
            countOccurrences(
                value,
                "\uFFFD"
            );

        return score;
    }

    private int countOccurrences(
        String value,
        String needle
    ) {

        if (
            value == null
                || value.isEmpty()
                || needle == null
                || needle.isEmpty()
        ) {

            return 0;
        }

        int count =
            0;

        int index =
            0;

        while (
            (
                index =
                    value.indexOf(
                        needle,
                        index
                    )
            ) >= 0
        ) {

            count++;

            index +=
                needle.length();
        }

        return count;
    }

    // =========================================================
    // HTML SAFETY
    // =========================================================

    private String escapeHtml(
        String value
    ) {

        if (
            value == null
        ) {

            return "";
        }

        return value
            .replace(
                "&",
                "&amp;"
            )
            .replace(
                "<",
                "&lt;"
            )
            .replace(
                ">",
                "&gt;"
            )
            .replace(
                "\"",
                "&quot;"
            )
            .replace(
                "'",
                "&#39;"
            );
    }

    // =========================================================
    // SAFE HELPERS
    // =========================================================

    private Long safeReminderId(
        AppointmentReminder reminder
    ) {

        return reminder != null
            ? reminder.getId()
            : null;
    }

    private OffsetDateTime safeAppointmentStartAt(
        AppointmentReminder reminder
    ) {

        return reminder != null
            ? reminder.getAppointmentStartAt()
            : null;
    }

    private String safeIdempotencyKey(
        String idempotencyKey
    ) {

        String normalized =
            normalizeNullable(
                idempotencyKey
            );

        if (
            normalized == null
        ) {

            return null;
        }

        return truncate(
            normalized,
            MAX_IDEMPOTENCY_KEY_LENGTH
        );
    }

    private String safeExceptionMessage(
        Exception exception
    ) {

        if (
            exception == null
        ) {

            return "Error no especificado al contactar con Resend.";
        }

        String message =
            normalizeNullable(
                exception.getMessage()
            );

        if (
            message == null
        ) {

            message =
                exception
                    .getClass()
                    .getSimpleName();
        }

        if (
            apiKey != null
                && message.contains(
                    apiKey
                )
        ) {

            message =
                message.replace(
                    apiKey,
                    "[REDACTED]"
                );
        }

        return truncate(
            message,
            MAX_SAFE_ERROR_LENGTH
        );
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private String normalizeBaseUrl(
        String value
    ) {

        String normalized =
            normalizeNullable(
                value
            );

        if (
            normalized == null
        ) {

            return null;
        }

        while (
            normalized.endsWith(
                "/"
            )
        ) {

            normalized =
                normalized.substring(
                    0,
                    normalized.length() - 1
                );
        }

        return normalized;
    }

    private String normalizeNullable(
        String value
    ) {

        if (
            value == null
        ) {

            return null;
        }

        String normalized =
            value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    private String truncate(
        String value,
        int maxLength
    ) {

        if (
            value == null
        ) {

            return null;
        }

        if (
            value.length()
                <= maxLength
        ) {

            return value;
        }

        return value.substring(
            0,
            maxLength
        );
    }
}