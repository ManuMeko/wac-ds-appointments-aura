package com.wacds.appointments.publicbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Solicitud pública de creación de una reserva.
 *
 * Este DTO representa exclusivamente los datos que un cliente
 * externo puede proporcionar desde la web pública.
 *
 * =========================================================
 * NO EXPONE
 * =========================================================
 *
 * El cliente público NO controla:
 *
 * - customerId;
 * - appointmentId;
 * - appointment status;
 * - endAt;
 * - duración;
 * - estado de confirmación;
 * - management token;
 * - reminders;
 * - timestamps internos;
 * - operaciones administrativas.
 *
 * =========================================================
 * RESPONSABILIDAD DEL BACKEND
 * =========================================================
 *
 * PublicBookingService será responsable de:
 *
 * - validar Business;
 * - validar Professional;
 * - validar Service;
 * - comprobar aislamiento multi-tenant;
 * - validar Professional <-> Service;
 * - cargar BusinessSettings;
 * - aplicar minimumBookingNoticeMinutes;
 * - aplicar bookingHorizonDays;
 * - consultar disponibilidad;
 * - aplicar excepciones de agenda;
 * - aplicar buffers;
 * - comprobar solapamientos;
 * - aplicar bloqueo de concurrencia;
 * - resolver o crear Customer;
 * - calcular endAt;
 * - decidir el estado inicial;
 * - persistir Appointment;
 * - generar management token;
 * - sincronizar AppointmentReminder.
 *
 * =========================================================
 * PAYLOAD HTTP
 * =========================================================
 *
 * Ejemplo:
 *
 * {
 *   "businessId": 1,
 *   "professionalId": 1,
 *   "serviceId": 1,
 *   "startAt": "2026-10-19T11:20:00+02:00",
 *   "firstName": "Natalia",
 *   "lastName": "García",
 *   "email": "natalia@example.com",
 *   "phone": "+34600000000",
 *   "notes": "Prueba E2E reminders"
 * }
 */
public record PublicBookingRequest(

    // =========================================================
    // BUSINESS
    // =========================================================

    @NotNull(
        message = "businessId es obligatorio."
    )
    @Positive(
        message = "businessId debe ser mayor que 0."
    )
    Long businessId,

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    @NotNull(
        message = "professionalId es obligatorio."
    )
    @Positive(
        message = "professionalId debe ser mayor que 0."
    )
    Long professionalId,

    // =========================================================
    // SERVICE
    // =========================================================

    @NotNull(
        message = "serviceId es obligatorio."
    )
    @Positive(
        message = "serviceId debe ser mayor que 0."
    )
    Long serviceId,

    // =========================================================
    // APPOINTMENT START
    // =========================================================

    /**
     * Inicio solicitado por el cliente.
     *
     * El cliente únicamente selecciona startAt.
     *
     * endAt será calculado por el backend a partir del slot
     * seleccionado y de la duración del Service.
     *
     * Ejemplo:
     *
     * 2026-10-19T11:20:00+02:00
     */
    @NotNull(
        message = "startAt es obligatorio."
    )
    OffsetDateTime startAt,

    // =========================================================
    // CUSTOMER
    // =========================================================

    @NotBlank(
        message = "El nombre es obligatorio."
    )
    @Size(
        max = 100,
        message = "El nombre no puede superar los 100 caracteres."
    )
    String firstName,

    @Size(
        max = 150,
        message = "Los apellidos no pueden superar los 150 caracteres."
    )
    String lastName,

    /**
     * Email del cliente.
     *
     * Actualmente es opcional a nivel DTO.
     *
     * Si existe y los reminders EMAIL están habilitados,
     * AppointmentReminderService podrá utilizarlo como destino.
     */
    @Email(
        message = "El email no tiene un formato válido."
    )
    @Size(
        max = 180,
        message = "El email no puede superar los 180 caracteres."
    )
    String email,

    @Size(
        max = 40,
        message = "El teléfono no puede superar los 40 caracteres."
    )
    String phone,

    // =========================================================
    // NOTES
    // =========================================================

    @Size(
        max = 2000,
        message = "Las notas no pueden superar los 2000 caracteres."
    )
    String notes

) {

    // =========================================================
    // NORMALIZATION
    // =========================================================

    /**
     * Compact constructor del record.
     *
     * Normalizamos los valores de entrada antes de que el DTO
     * llegue a PublicBookingService.
     *
     * No se modifica startAt:
     *
     * conservar el OffsetDateTime recibido es importante para
     * poder validar correctamente el instante solicitado contra
     * la zona horaria del Business.
     */
    public PublicBookingRequest {

        firstName =
            normalizeRequiredText(
                firstName
            );

        lastName =
            normalizeOptionalText(
                lastName
            );

        email =
            normalizeEmail(
                email
            );

        phone =
            normalizeOptionalText(
                phone
            );

        notes =
            normalizeOptionalText(
                notes
            );
    }

    // =========================================================
    // REQUIRED TEXT
    // =========================================================

    /**
     * Normaliza un campo obligatorio.
     *
     * Jakarta Validation será responsable posteriormente de
     * rechazar null mediante @NotBlank.
     */
    private static String normalizeRequiredText(
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

    // =========================================================
    // OPTIONAL TEXT
    // =========================================================

    /**
     * Convierte cadenas vacías o formadas únicamente por
     * espacios en null.
     */
    private static String normalizeOptionalText(
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

    // =========================================================
    // EMAIL
    // =========================================================

    /**
     * Normaliza el email para evitar identidades distintas por
     * diferencias exclusivamente de mayúsculas/minúsculas.
     *
     * Ejemplo:
     *
     * Natalia@Example.COM
     *
     * pasa a:
     *
     * natalia@example.com
     */
    private static String normalizeEmail(
        String value
    ) {

        String normalized =
            normalizeOptionalText(
                value
            );

        if (
            normalized == null
        ) {

            return null;
        }

        return normalized.toLowerCase(
            Locale.ROOT
        );
    }
}