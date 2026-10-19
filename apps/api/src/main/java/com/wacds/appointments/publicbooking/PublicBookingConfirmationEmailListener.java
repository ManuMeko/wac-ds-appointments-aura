package com.wacds.appointments.publicbooking;

import com.wacds.appointments.publicbooking.PublicBookingService.PublicBookingCreatedEvent;
import com.wacds.appointments.reminder.ReminderDeliveryService.DeliveryResult;
import com.wacds.appointments.reminder.ResendEmailReminderDeliveryService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Locale;

/**
 * Envía el email de confirmación de una reserva pública
 * después de que la transacción de creación haya finalizado
 * correctamente.
 *
 * =========================================================
 * ARQUITECTURA
 * =========================================================
 *
 * PublicBookingService
 *      ↓
 * publica PublicBookingCreatedEvent
 *      ↓
 * COMMIT correcto
 *      ↓
 * PublicBookingConfirmationEmailListener
 *      ↓
 * ResendEmailReminderDeliveryService
 *      ↓
 * Resend
 *
 * =========================================================
 * GARANTÍA TRANSACCIONAL
 * =========================================================
 *
 * Se utiliza:
 *
 * TransactionPhase.AFTER_COMMIT
 *
 * Por tanto:
 *
 * - primero se guarda la reserva;
 * - primero se guarda el management token;
 * - primero se crean/sincronizan reminders;
 * - después se intenta enviar el email.
 *
 * Si Resend falla:
 *
 * - NO se revierte la reserva;
 * - NO se elimina la cita;
 * - NO se revierte el transaction commit.
 *
 * =========================================================
 * SEGURIDAD DEL MANAGEMENT TOKEN
 * =========================================================
 *
 * El token nunca:
 *
 * - se escribe en logs;
 * - se persiste aquí;
 * - se utiliza en la idempotency key;
 * - aparece en mensajes de error.
 *
 * El enlace enviado por email tiene esta forma:
 *
 * /reservar/gestionar?appointmentId=123#token=SECRET
 *
 * El token viaja dentro del fragment URL.
 *
 * El fragment:
 *
 * - no se envía al servidor HTTP;
 * - no forma parte de la petición al backend;
 * - será recuperado por el frontend;
 * - será almacenado temporalmente en sessionStorage;
 * - deberá eliminarse después de la URL.
 *
 * =========================================================
 * PROVIDER OPTIONALITY
 * =========================================================
 *
 * ResendEmailReminderDeliveryService es condicional.
 *
 * Cuando:
 *
 * app.reminders.email.real-delivery-enabled=false
 *
 * el bean Resend puede no existir.
 *
 * Por eso se utiliza ObjectProvider y la ausencia del proveedor
 * NO impide arrancar Spring Boot.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "app.public-booking.confirmation-email",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PublicBookingConfirmationEmailListener {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern(
            "EEEE d 'de' MMMM 'de' yyyy 'a las' HH:mm",
            new Locale(
                "es",
                "ES"
            )
        );

    private static final String IDEMPOTENCY_PREFIX =
        "public-booking-confirmation/";

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final ObjectProvider<ResendEmailReminderDeliveryService>
        resendProvider;

    // =========================================================
    // CONFIGURATION
    // =========================================================

    private final String frontendBaseUrl;

    private final String managementPath;

    private final String configuredSubject;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public PublicBookingConfirmationEmailListener(

        ObjectProvider<ResendEmailReminderDeliveryService>
            resendProvider,

        @Value(
            "${app.public-booking.frontend-base-url:http://localhost:3000}"
        )
        String frontendBaseUrl,

        @Value(
            "${app.public-booking.management-path:/reservar/gestionar}"
        )
        String managementPath,

        @Value(
            "${app.public-booking.confirmation-email.subject:Tu reserva está registrada}"
        )
        String configuredSubject

    ) {

        this.resendProvider =
            resendProvider;

        this.frontendBaseUrl =
            normalizeBaseUrl(
                frontendBaseUrl
            );

        this.managementPath =
            normalizeManagementPath(
                managementPath
            );

        this.configuredSubject =
            normalizeNullable(
                configuredSubject
            );
    }

    // =========================================================
    // AFTER COMMIT
    // =========================================================

    /**
     * Se ejecuta únicamente cuando la transacción que creó
     * la reserva terminó correctamente.
     */
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleBookingCreated(
        PublicBookingCreatedEvent event
    ) {

        if (
            event == null
        ) {

            log.warn(
                "Public booking confirmation email skipped: "
                    + "event is null."
            );

            return;
        }

        Long appointmentId =
            event.appointmentId();

        if (
            appointmentId == null
                || appointmentId <= 0
        ) {

            log.warn(
                "Public booking confirmation email skipped: "
                    + "invalid appointmentId."
            );

            return;
        }

        if (
            isBlank(
                event.customerEmail()
            )
        ) {

            log.info(
                "Public booking confirmation email skipped. "
                    + "appointmentId={}, reason=no-email",
                appointmentId
            );

            return;
        }

        if (
            isBlank(
                event.managementToken()
            )
        ) {

            /*
             * Nunca incluimos el token inválido en el log.
             */
            log.warn(
                "Public booking confirmation email skipped. "
                    + "appointmentId={}, reason=missing-management-token",
                appointmentId
            );

            return;
        }

        ResendEmailReminderDeliveryService resendService =
            resendProvider
                .getIfAvailable();

        if (
            resendService == null
        ) {

            log.info(
                "Public booking confirmation email skipped. "
                    + "appointmentId={}, reason=resend-provider-disabled",
                appointmentId
            );

            return;
        }

        try {

            String managementUrl =
                buildManagementUrl(
                    event
                );

            String subject =
                buildSubject(
                    event
                );

            String html =
                buildHtml(
                    event,
                    managementUrl
                );

            String idempotencyKey =
                buildIdempotencyKey(
                    event
                );

            DeliveryResult result =
                resendService
                    .sendTransactionalEmail(
                        event.customerEmail(),
                        subject,
                        html,
                        idempotencyKey
                    );

            handleDeliveryResult(
                event,
                result
            );

        } catch (
            Exception exception
        ) {

            /*
             * Muy importante:
             *
             * Estamos AFTER_COMMIT.
             *
             * Un error del email no puede revertir la reserva.
             *
             * Tampoco imprimimos:
             *
             * - managementToken;
             * - managementUrl;
             * - email completo.
             */
            log.warn(
                "Public booking confirmation email failed. "
                    + "appointmentId={}, error={}",
                appointmentId,
                safeExceptionMessage(
                    exception
                )
            );
        }
    }

    // =========================================================
    // DELIVERY RESULT
    // =========================================================

    private void handleDeliveryResult(
        PublicBookingCreatedEvent event,
        DeliveryResult result
    ) {

        Long appointmentId =
            event.appointmentId();

        if (
            result == null
        ) {

            log.warn(
                "Public booking confirmation email returned "
                    + "no delivery result. appointmentId={}",
                appointmentId
            );

            return;
        }

        if (
            result.success()
        ) {

            log.info(
                "Public booking confirmation email sent. "
                    + "appointmentId={}, providerMessageId={}",
                appointmentId,
                normalizeNullable(
                    result.providerMessageId()
                )
            );

            return;
        }

        log.warn(
            "Public booking confirmation email rejected. "
                + "appointmentId={}, error={}",
            appointmentId,
            normalizeSafeError(
                result.errorMessage()
            )
        );
    }

    // =========================================================
    // MANAGEMENT URL
    // =========================================================

    /**
     * Ejemplo:
     *
     * http://localhost:3000/reservar/gestionar
     * ?appointmentId=23
     * #token=SECRET
     *
     * El managementToken:
     *
     * - NO forma parte de la query string;
     * - NO forma parte de la idempotency key;
     * - NO se registra.
     */
    private String buildManagementUrl(
        PublicBookingCreatedEvent event
    ) {

        validateEventForManagementLink(
            event
        );

        String encodedToken =
            URLEncoder.encode(
                event
                    .managementToken()
                    .trim(),
                StandardCharsets.UTF_8
            );

        return frontendBaseUrl
            + managementPath
            + "?appointmentId="
            + event.appointmentId()
            + "#token="
            + encodedToken;
    }

    private void validateEventForManagementLink(
        PublicBookingCreatedEvent event
    ) {

        if (
            event == null
        ) {

            throw new IllegalArgumentException(
                "El evento de reserva es obligatorio."
            );
        }

        if (
            event.appointmentId() == null
                || event.appointmentId() <= 0
        ) {

            throw new IllegalArgumentException(
                "El appointmentId de la reserva no es válido."
            );
        }

        if (
            isBlank(
                event.managementToken()
            )
        ) {

            throw new IllegalArgumentException(
                "La reserva no contiene una credencial "
                    + "de gestión válida."
            );
        }

        if (
            frontendBaseUrl == null
        ) {

            throw new IllegalStateException(
                "La URL pública del frontend no está configurada."
            );
        }

        if (
            managementPath == null
        ) {

            throw new IllegalStateException(
                "La ruta pública de gestión no está configurada."
            );
        }
    }

    // =========================================================
    // SUBJECT
    // =========================================================

    private String buildSubject(
        PublicBookingCreatedEvent event
    ) {

        String baseSubject =
            configuredSubject != null
                ? configuredSubject
                : "Tu reserva está registrada";

        String businessName =
            normalizeNullable(
                event.businessName()
            );

        if (
            businessName == null
        ) {

            return baseSubject;
        }

        return baseSubject
            + " · "
            + businessName;
    }

    // =========================================================
    // HTML
    // =========================================================

    private String buildHtml(
        PublicBookingCreatedEvent event,
        String managementUrl
    ) {

        String businessName =
            escapeHtml(
                fallback(
                    event.businessName(),
                    "Tu centro"
                )
            );

        String customerName =
            escapeHtml(
                fallback(
                    event.customerName(),
                    "Cliente"
                )
            );

        String serviceName =
            escapeHtml(
                fallback(
                    event.serviceName(),
                    "Servicio"
                )
            );

        String professionalName =
            escapeHtml(
                fallback(
                    event.professionalName(),
                    "Profesional"
                )
            );

        String formattedStartAt =
            escapeHtml(
                formatDateTime(
                    event.startAt()
                )
            );

        String escapedManagementUrl =
            escapeHtml(
                managementUrl
            );

        String appointmentReference =
            event.appointmentId() != null
                ? "#"
                    + event.appointmentId()
                : "-";

        return """
            <!doctype html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta
                    name="viewport"
                    content="width=device-width, initial-scale=1.0"
                >
                <meta
                    name="color-scheme"
                    content="light"
                >
                <title>Reserva recibida</title>
            </head>

            <body style="
                margin:0;
                padding:0;
                background:#f4f0eb;
                font-family:Arial,Helvetica,sans-serif;
                color:#332d29;
            ">

                <table
                    role="presentation"
                    width="100%%"
                    cellspacing="0"
                    cellpadding="0"
                    border="0"
                    style="
                        width:100%%;
                        background:#f4f0eb;
                        padding:36px 16px;
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
                                    width:100%%;
                                    max-width:620px;
                                    background:#ffffff;
                                    border:1px solid #e9e1d8;
                                    border-radius:22px;
                                    overflow:hidden;
                                "
                            >

                                <tr>
                                    <td style="
                                        padding:
                                            38px
                                            38px
                                            20px
                                            38px;
                                    ">

                                        <div style="
                                            margin-bottom:15px;
                                            color:#9b887a;
                                            font-size:11px;
                                            font-weight:600;
                                            letter-spacing:2.3px;
                                            text-transform:uppercase;
                                        ">
                                            %s
                                        </div>

                                        <h1 style="
                                            margin:0;
                                            color:#302a26;
                                            font-size:30px;
                                            line-height:1.2;
                                            font-weight:600;
                                        ">
                                            Hemos recibido tu reserva
                                        </h1>

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:
                                            4px
                                            38px
                                            24px
                                            38px;
                                        color:#62564e;
                                        font-size:16px;
                                        line-height:1.7;
                                    ">

                                        Hola %s,

                                        <br><br>

                                        Tu cita se ha registrado
                                        correctamente. Aquí tienes
                                        todos los detalles.

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:
                                            0
                                            38px
                                            22px
                                            38px;
                                    ">

                                        <div style="
                                            padding:24px;
                                            background:#f8f4ef;
                                            border:1px solid #ebe2d9;
                                            border-radius:16px;
                                        ">

                                            <table
                                                role="presentation"
                                                width="100%%"
                                                cellspacing="0"
                                                cellpadding="0"
                                                border="0"
                                            >

                                                <tr>
                                                    <td style="
                                                        padding-bottom:18px;
                                                    ">
                                                        <div style="
                                                            margin-bottom:5px;
                                                            color:#a08e80;
                                                            font-size:11px;
                                                            letter-spacing:1.2px;
                                                            text-transform:uppercase;
                                                        ">
                                                            Servicio
                                                        </div>

                                                        <div style="
                                                            color:#342e2a;
                                                            font-size:18px;
                                                            font-weight:600;
                                                        ">
                                                            %s
                                                        </div>
                                                    </td>
                                                </tr>

                                                <tr>
                                                    <td style="
                                                        padding-bottom:18px;
                                                    ">
                                                        <div style="
                                                            margin-bottom:5px;
                                                            color:#a08e80;
                                                            font-size:11px;
                                                            letter-spacing:1.2px;
                                                            text-transform:uppercase;
                                                        ">
                                                            Profesional
                                                        </div>

                                                        <div style="
                                                            color:#51463f;
                                                            font-size:15px;
                                                        ">
                                                            %s
                                                        </div>
                                                    </td>
                                                </tr>

                                                <tr>
                                                    <td style="
                                                        padding-bottom:18px;
                                                    ">
                                                        <div style="
                                                            margin-bottom:5px;
                                                            color:#a08e80;
                                                            font-size:11px;
                                                            letter-spacing:1.2px;
                                                            text-transform:uppercase;
                                                        ">
                                                            Fecha y hora
                                                        </div>

                                                        <div style="
                                                            color:#342e2a;
                                                            font-size:17px;
                                                            font-weight:600;
                                                        ">
                                                            %s
                                                        </div>
                                                    </td>
                                                </tr>

                                                <tr>
                                                    <td>
                                                        <div style="
                                                            margin-bottom:5px;
                                                            color:#a08e80;
                                                            font-size:11px;
                                                            letter-spacing:1.2px;
                                                            text-transform:uppercase;
                                                        ">
                                                            Referencia
                                                        </div>

                                                        <div style="
                                                            color:#51463f;
                                                            font-size:14px;
                                                        ">
                                                            %s
                                                        </div>
                                                    </td>
                                                </tr>

                                            </table>

                                        </div>

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:
                                            4px
                                            38px
                                            16px
                                            38px;
                                        color:#62564e;
                                        font-size:15px;
                                        line-height:1.7;
                                    ">

                                        Desde el siguiente enlace puedes
                                        consultar tu reserva y, cuando
                                        las condiciones del negocio lo
                                        permitan, cambiar el horario o
                                        cancelarla.

                                    </td>
                                </tr>

                                <tr>
                                    <td
                                        align="center"
                                        style="
                                            padding:
                                                10px
                                                38px
                                                32px
                                                38px;
                                        "
                                    >

                                        <a
                                            href="%s"
                                            target="_blank"
                                            rel="noreferrer noopener"
                                            style="
                                                display:inline-block;
                                                padding:
                                                    15px
                                                    26px;
                                                background:#332d29;
                                                color:#ffffff;
                                                text-decoration:none;
                                                border-radius:999px;
                                                font-size:14px;
                                                font-weight:600;
                                                letter-spacing:0.2px;
                                            "
                                        >
                                            Gestionar mi cita
                                        </a>

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:
                                            0
                                            38px
                                            32px
                                            38px;
                                        color:#88796f;
                                        font-size:12px;
                                        line-height:1.6;
                                    ">

                                        Este enlace es personal.
                                        No lo compartas con otras
                                        personas, ya que permite
                                        gestionar tu reserva.

                                    </td>
                                </tr>

                                <tr>
                                    <td style="
                                        padding:
                                            22px
                                            38px;
                                        background:#332d29;
                                        color:#eee7e1;
                                        font-size:12px;
                                        line-height:1.6;
                                    ">

                                        %s · Sistema de reservas

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
                businessName,
                customerName,
                serviceName,
                professionalName,
                formattedStartAt,
                escapeHtml(
                    appointmentReference
                ),
                escapedManagementUrl,
                businessName
            );
    }

    // =========================================================
    // IDEMPOTENCY
    // =========================================================

    /**
     * La clave NO contiene:
     *
     * - managementToken;
     * - email;
     * - datos personales.
     *
     * Una reserva solo debe generar una confirmación inicial.
     */
    private String buildIdempotencyKey(
        PublicBookingCreatedEvent event
    ) {

        return IDEMPOTENCY_PREFIX
            + event.appointmentId();
    }

    // =========================================================
    // DATE
    // =========================================================

    private String formatDateTime(
        OffsetDateTime value
    ) {

        if (
            value == null
        ) {

            return "Fecha pendiente";
        }

        return DATE_TIME_FORMATTER
            .format(
                value
            );
    }

    // =========================================================
    // FRONTEND CONFIGURATION
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

    private String normalizeManagementPath(
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

        if (
            !normalized.startsWith(
                "/"
            )
        ) {

            normalized =
                "/"
                    + normalized;
        }

        while (
            normalized.length() > 1
                && normalized.endsWith(
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
    // SAFE ERROR HANDLING
    // =========================================================

    private String safeExceptionMessage(
        Exception exception
    ) {

        if (
            exception == null
        ) {

            return "Error no especificado.";
        }

        String message =
            normalizeNullable(
                exception.getMessage()
            );

        if (
            message == null
        ) {

            return exception
                .getClass()
                .getSimpleName();
        }

        /*
         * Nunca devolvemos mensajes excesivamente largos.
         *
         * Tampoco incluimos el event ni managementUrl en logs,
         * por lo que el token no debería poder aparecer aquí.
         */
        if (
            message.length()
                > 500
        ) {

            return message.substring(
                0,
                500
            );
        }

        return message;
    }

    private String normalizeSafeError(
        String value
    ) {

        String normalized =
            normalizeNullable(
                value
            );

        if (
            normalized == null
        ) {

            return "Error de entrega no especificado.";
        }

        if (
            normalized.length()
                > 500
        ) {

            return normalized.substring(
                0,
                500
            );
        }

        return normalized;
    }

    // =========================================================
    // GENERIC HELPERS
    // =========================================================

    private String fallback(
        String value,
        String fallback
    ) {

        String normalized =
            normalizeNullable(
                value
            );

        return normalized != null
            ? normalized
            : fallback;
    }

    private boolean isBlank(
        String value
    ) {

        return value == null
            || value.isBlank();
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
}