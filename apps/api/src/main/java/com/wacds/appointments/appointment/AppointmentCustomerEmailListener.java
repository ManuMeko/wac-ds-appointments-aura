package com.wacds.appointments.appointment;

import com.wacds.appointments.appointment.AppointmentService.AppointmentCustomerEmailEvent;
import com.wacds.appointments.appointment.AppointmentService.AppointmentCustomerEmailKind;
import com.wacds.appointments.reminder.ReminderDeliveryService.DeliveryResult;
import com.wacds.appointments.reminder.ResendEmailReminderDeliveryService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Envía al cliente el aviso de una cita creada, reprogramada
 * o cancelada desde el panel, después del commit.
 *
 * Reutiliza ResendEmailReminderDeliveryService.sendTransactionalEmail,
 * el mismo método que la confirmación de la reserva pública.
 * Si el envío falla, la cita ya confirmada en base de datos
 * no se revierte.
 *
 * El managementToken, cuando esta operación lo acaba de emitir,
 * viaja solo en el HTML y dentro del fragmento de la URL:
 *
 * /reservar/gestionar?appointmentId=123#token=SECRET
 */
@Slf4j
@Component
public class AppointmentCustomerEmailListener {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern(
            "EEEE d 'de' MMMM 'de' yyyy 'a las' HH:mm",
            Locale.of(
                "es",
                "ES"
            )
        );

    private static final String CONFIRMATION_PREFIX =
        "panel-appointment-confirmation/";

    private static final String RESCHEDULE_PREFIX =
        "panel-appointment-reschedule/";

    private static final String CANCELLATION_PREFIX =
        "panel-appointment-cancellation/";

    private final ObjectProvider<ResendEmailReminderDeliveryService>
        resendProvider;

    private final String frontendBaseUrl;

    private final String managementPath;

    private final String confirmationSubject;

    public AppointmentCustomerEmailListener(

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
        String confirmationSubject

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

        this.confirmationSubject =
            normalizeNullable(
                confirmationSubject
            );
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleCustomerEmail(
        AppointmentCustomerEmailEvent event
    ) {

        if (
            event == null
                || event.kind() == null
        ) {

            log.warn(
                "Panel appointment email skipped: "
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
                "Panel appointment email skipped: "
                    + "invalid appointmentId. kind={}",
                event.kind()
            );

            return;
        }

        if (
            isBlank(
                event.customerEmail()
            )
        ) {

            log.info(
                "Panel appointment email skipped. "
                    + "appointmentId={}, kind={}, reason=no-email",
                appointmentId,
                event.kind()
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
                "Panel appointment email skipped. "
                    + "appointmentId={}, kind={}, reason=resend-provider-disabled",
                appointmentId,
                event.kind()
            );

            return;
        }

        try {

            String managementUrl =
                buildManagementUrl(
                    event
                );

            DeliveryResult result =
                resendService
                    .sendTransactionalEmail(
                        event.customerEmail(),
                        buildSubject(
                            event
                        ),
                        buildHtml(
                            event,
                            managementUrl
                        ),
                        buildIdempotencyKey(
                            event
                        )
                    );

            handleDeliveryResult(
                event,
                result
            );

        } catch (
            Exception exception
        ) {

            log.warn(
                "Panel appointment email failed. "
                    + "appointmentId={}, kind={}, error={}",
                appointmentId,
                event.kind(),
                safeExceptionMessage(
                    exception
                )
            );
        }
    }

    private void handleDeliveryResult(
        AppointmentCustomerEmailEvent event,
        DeliveryResult result
    ) {

        Long appointmentId =
            event.appointmentId();

        if (
            result == null
        ) {

            log.warn(
                "Panel appointment email returned "
                    + "no delivery result. appointmentId={}, kind={}",
                appointmentId,
                event.kind()
            );

            return;
        }

        if (
            result.success()
        ) {

            log.info(
                "Panel appointment email sent. "
                    + "appointmentId={}, kind={}, providerMessageId={}",
                appointmentId,
                event.kind(),
                normalizeNullable(
                    result.providerMessageId()
                )
            );

            return;
        }

        log.warn(
            "Panel appointment email rejected. "
                + "appointmentId={}, kind={}, error={}",
            appointmentId,
            event.kind(),
            normalizeSafeError(
                result.errorMessage()
            )
        );
    }

    private String buildSubject(
        AppointmentCustomerEmailEvent event
    ) {

        String baseSubject =
            switch (event.kind()) {

                case CONFIRMATION ->
                    confirmationSubject != null
                        ? confirmationSubject
                        : "Tu reserva está registrada";

                case RESCHEDULE ->
                    "Tu cita ha cambiado de horario";

                case CANCELLATION ->
                    "Tu cita ha sido cancelada";
            };

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

    private String buildIdempotencyKey(
        AppointmentCustomerEmailEvent event
    ) {

        return switch (event.kind()) {

            case CONFIRMATION ->
                CONFIRMATION_PREFIX
                    + event.appointmentId();

            case RESCHEDULE ->
                RESCHEDULE_PREFIX
                    + event.appointmentId()
                    + "/"
                    + startInstantKey(
                        event.startAt()
                    );

            case CANCELLATION ->
                CANCELLATION_PREFIX
                    + event.appointmentId();
        };
    }

    private String startInstantKey(
        OffsetDateTime startAt
    ) {

        if (
            startAt == null
        ) {

            return "unscheduled";
        }

        return Long.toString(
            startAt
                .toInstant()
                .toEpochMilli()
        );
    }

    /**
     * Misma forma que el email público. Si no hay token
     * en claro, el aviso se envía sin el enlace.
     */
    private String buildManagementUrl(
        AppointmentCustomerEmailEvent event
    ) {

        if (
            isBlank(
                event.managementToken()
            )
                || frontendBaseUrl == null
                || managementPath == null
                || event.appointmentId() == null
        ) {

            return null;
        }

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

    private String buildHtml(
        AppointmentCustomerEmailEvent event,
        String managementUrl
    ) {

        Copy copy =
            copyFor(
                event.kind()
            );

        String businessName =
            escapeHtml(
                fallback(
                    event.businessName(),
                    "Tu centro"
                )
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
                <title>%1$s</title>
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
                                            %2$s
                                        </div>

                                        <h1 style="
                                            margin:0;
                                            color:#302a26;
                                            font-size:30px;
                                            line-height:1.2;
                                            font-weight:600;
                                        ">
                                            %1$s
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

                                        Hola %3$s,

                                        <br><br>

                                        %4$s

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
                                                            %5$s
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
                                                            %6$s
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
                                                            %7$s
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
                                                            %8$s
                                                        </div>
                                                    </td>
                                                </tr>

                                            </table>

                                        </div>

                                    </td>
                                </tr>

                                %9$s

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

                                        %2$s · Sistema de reservas

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
                escapeHtml(
                    copy.title()
                ),
                businessName,
                escapeHtml(
                    fallback(
                        event.customerName(),
                        "Cliente"
                    )
                ),
                escapeHtml(
                    copy.intro()
                ),
                escapeHtml(
                    fallback(
                        event.serviceName(),
                        "Servicio"
                    )
                ),
                escapeHtml(
                    fallback(
                        event.professionalName(),
                        "Profesional"
                    )
                ),
                escapeHtml(
                    formatDateTime(
                        inBusinessZone(
                            event
                        )
                    )
                ),
                escapeHtml(
                    appointmentReference
                ),
                managementSection(
                    managementUrl,
                    copy
                )
            );
    }

    private String managementSection(
        String managementUrl,
        Copy copy
    ) {

        if (
            isBlank(
                managementUrl
            )
        ) {

            return """
                <tr>
                    <td style="
                        padding:
                            4px
                            38px
                            32px
                            38px;
                        color:#62564e;
                        font-size:15px;
                        line-height:1.7;
                    ">

                        %s

                    </td>
                </tr>
                """
                .formatted(
                    escapeHtml(
                        copy.withoutLink()
                    )
                );
        }

        String escapedManagementUrl =
            escapeHtml(
                managementUrl
            );

        return """
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

                    %s

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
                        %s
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
            """
            .formatted(
                escapeHtml(
                    copy.withLink()
                ),
                escapedManagementUrl,
                escapeHtml(
                    copy.buttonLabel()
                )
            );
    }

    private Copy copyFor(
        AppointmentCustomerEmailKind kind
    ) {

        return switch (kind) {

            case CONFIRMATION ->
                new Copy(
                    "Hemos recibido tu reserva",
                    "Tu cita se ha registrado correctamente. "
                        + "Aquí tienes todos los detalles.",
                    "Desde el siguiente enlace puedes consultar "
                        + "tu reserva y, cuando las condiciones "
                        + "del negocio lo permitan, cambiar el "
                        + "horario o cancelarla.",
                    "Gestionar mi cita",
                    "Tu cita quedó registrada. "
                        + "Si más adelante recibes un enlace "
                        + "de gestión, podrás consultar el horario."
                );

            case RESCHEDULE ->
                new Copy(
                    "Tu cita ha cambiado de horario",
                    "Hemos actualizado la fecha y hora de tu cita. "
                        + "Este es el nuevo horario.",
                    "Desde el siguiente enlace puedes consultar "
                        + "tu reserva con el nuevo horario.",
                    "Gestionar mi cita",
                    "Si ya dispones de un enlace de gestión, "
                        + "puedes usarlo para consultar el nuevo horario."
                );

            case CANCELLATION ->
                new Copy(
                    "Tu cita ha sido cancelada",
                    "Te confirmamos que la siguiente cita "
                        + "ha quedado cancelada.",
                    "Si necesitas consultar los datos de la cita, "
                        + "puedes usar tu enlace de gestión.",
                    "Consultar mi cita",
                    "No es necesario que hagas nada más. "
                        + "La cita ya no está programada."
                );
        };
    }

    private OffsetDateTime inBusinessZone(
        AppointmentCustomerEmailEvent event
    ) {

        OffsetDateTime startAt =
            event.startAt();

        if (
            startAt == null
                || isBlank(
                    event.timezone()
                )
        ) {

            return startAt;
        }

        try {

            return startAt
                .atZoneSameInstant(
                    ZoneId.of(
                        event.timezone().trim()
                    )
                )
                .toOffsetDateTime();

        } catch (
            DateTimeException exception
        ) {

            return startAt;
        }
    }

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

    private record Copy(
        String title,
        String intro,
        String withLink,
        String buttonLabel,
        String withoutLink
    ) {
    }
}
