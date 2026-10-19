package com.wacds.appointments.appointment.dto;

import com.wacds.appointments.appointment.Appointment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AppointmentResponse(

    Long id,

    Long businessId,

    Long customerId,

    String customerName,

    String customerEmail,

    String customerPhone,

    Long professionalId,

    String professionalName,

    Long serviceId,

    String serviceName,

    Integer serviceDurationMinutes,

    BigDecimal servicePrice,

    OffsetDateTime startAt,

    OffsetDateTime endAt,

    String timezone,

    Appointment.AppointmentStatus status,

    String notes,

    OffsetDateTime createdAt,

    OffsetDateTime updatedAt

) {
}