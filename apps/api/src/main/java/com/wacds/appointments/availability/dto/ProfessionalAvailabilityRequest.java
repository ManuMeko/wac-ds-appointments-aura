package com.wacds.appointments.availability.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ProfessionalAvailabilityRequest(

    @NotNull
    DayOfWeek dayOfWeek,

    @NotNull
    LocalTime startTime,

    @NotNull
    LocalTime endTime,

    Boolean active

) {
}