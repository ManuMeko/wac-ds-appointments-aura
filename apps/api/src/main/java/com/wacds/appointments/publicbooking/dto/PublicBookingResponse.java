package com.wacds.appointments.publicbooking.dto;

import com.wacds.appointments.appointment.Appointment;

import java.time.OffsetDateTime;

/**
 * Respuesta pública devuelta después de crear
 * correctamente una reserva.
 *
 * Este DTO constituye el contrato HTTP público que recibe
 * el cliente web una vez que PublicBookingService ha creado
 * correctamente una Appointment.
 *
 * =========================================================
 * OBJETIVO
 * =========================================================
 *
 * Permite que el frontend público pueda mostrar una pantalla
 * final de confirmación con:
 *
 * - identificador de la reserva;
 * - estado inicial;
 * - negocio;
 * - servicio;
 * - profesional;
 * - cliente;
 * - fecha y hora;
 * - zona horaria;
 * - mensaje de confirmación.
 *
 * Además entrega la credencial necesaria para la gestión
 * pública posterior de la reserva.
 *
 * =========================================================
 * SEGURIDAD
 * =========================================================
 *
 * Este DTO NO expone:
 *
 * - entidades JPA;
 * - Customer completo;
 * - identificadores internos innecesarios del Customer;
 * - datos privados adicionales del Customer;
 * - configuración interna del Business;
 * - información administrativa;
 * - tokenHash;
 * - credenciales Business;
 * - JWT;
 * - datos internos de auditoría.
 *
 * =========================================================
 * MANAGEMENT TOKEN
 * =========================================================
 *
 * managementToken contiene la credencial pública original
 * necesaria para gestionar posteriormente esta reserva.
 *
 * Este valor:
 *
 * - se genera criptográficamente;
 * - se devuelve exclusivamente al cliente que crea la cita;
 * - NO se almacena en texto plano en PostgreSQL;
 * - en base de datos únicamente se conserva su hash;
 * - debe tratarse como una credencial secreta.
 *
 * Permitirá posteriormente:
 *
 * - consultar la reserva;
 * - cancelarla;
 * - reprogramarla.
 *
 * IMPORTANTE:
 *
 * - nunca registrar managementToken en logs;
 * - nunca mostrarlo directamente al usuario;
 * - nunca utilizarlo como identificador visual;
 * - nunca incluirlo en listados públicos;
 * - nunca persistirlo en texto plano en backend.
 */
public record PublicBookingResponse(

    // =========================================================
    // BOOKING
    // =========================================================

    /**
     * Identificador interno de la Appointment creada.
     *
     * Puede utilizarse como referencia visual de la reserva,
     * pero NO constituye por sí mismo autorización para
     * consultarla, cancelarla o reprogramarla.
     */
    Long appointmentId,

    /**
     * Estado inicial de la Appointment.
     *
     * Según la configuración del Business podrá ser,
     * entre otros:
     *
     * - PENDING;
     * - CONFIRMED.
     */
    Appointment.AppointmentStatus status,

    // =========================================================
    // BUSINESS
    // =========================================================

    /**
     * Negocio propietario de la reserva.
     */
    Long businessId,

    /**
     * Nombre público del negocio.
     */
    String businessName,

    // =========================================================
    // SERVICE
    // =========================================================

    /**
     * Servicio reservado.
     */
    Long serviceId,

    /**
     * Nombre público del servicio.
     */
    String serviceName,

    /**
     * Duración efectiva del servicio en minutos.
     */
    Integer serviceDurationMinutes,

    // =========================================================
    // PROFESSIONAL
    // =========================================================

    /**
     * Profesional asignado a la reserva.
     */
    Long professionalId,

    /**
     * Nombre público del profesional.
     */
    String professionalName,

    // =========================================================
    // CUSTOMER
    // =========================================================

    /**
     * Nombre visible del cliente.
     *
     * No se expone la entidad Customer completa.
     */
    String customerName,

    // =========================================================
    // TIME
    // =========================================================

    /**
     * Inicio definitivo de la cita expresado mediante
     * OffsetDateTime.
     */
    OffsetDateTime startAt,

    /**
     * Final definitivo de la cita expresado mediante
     * OffsetDateTime.
     */
    OffsetDateTime endAt,

    /**
     * Zona horaria IANA utilizada por el negocio.
     *
     * Ejemplo:
     *
     * Europe/Madrid
     */
    String timezone,

    // =========================================================
    // PUBLIC MANAGEMENT
    // =========================================================

    /**
     * Credencial secreta utilizada para gestionar públicamente
     * esta Appointment sin necesidad de una cuenta Business.
     *
     * El valor original:
     *
     * - se entrega al cliente después de crear la reserva;
     * - nunca debe almacenarse en texto plano en backend;
     * - nunca debe escribirse en logs;
     * - nunca debe mostrarse directamente al usuario;
     * - nunca debe utilizarse como identificador público.
     *
     * El backend debe conservar exclusivamente la
     * representación segura correspondiente.
     */
    String managementToken,

    // =========================================================
    // CONFIRMATION
    // =========================================================

    /**
     * Mensaje público asociado al resultado de creación
     * de la reserva.
     *
     * Puede mostrarse directamente en la pantalla de
     * confirmación siempre que el servicio mantenga el mensaje
     * libre de información interna o sensible.
     */
    String message

) {
}