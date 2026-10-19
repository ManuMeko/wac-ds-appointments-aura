package com.wacds.appointments.appointment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record AppointmentRescheduleRequest(

    @NotNull(
        message = "La nueva fecha y hora de la cita son obligatorias."
    )
    OffsetDateTime startAt

) {
}