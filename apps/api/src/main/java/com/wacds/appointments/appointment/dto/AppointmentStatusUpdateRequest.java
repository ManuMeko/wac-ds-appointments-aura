package com.wacds.appointments.appointment.dto;

import com.wacds.appointments.appointment.Appointment;

import jakarta.validation.constraints.NotNull;

public record AppointmentStatusUpdateRequest(

    @NotNull(
        message = "El estado de la cita es obligatorio."
    )
    Appointment.AppointmentStatus status

) {
}