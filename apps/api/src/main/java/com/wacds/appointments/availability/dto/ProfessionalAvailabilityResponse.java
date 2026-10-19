package com.wacds.appointments.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record ProfessionalAvailabilityResponse(

    Long id,

    Long professionalId,

    DayOfWeek dayOfWeek,

    LocalTime startTime,

    LocalTime endTime,

    boolean active,

    OffsetDateTime createdAt,

    OffsetDateTime updatedAt

) {
}