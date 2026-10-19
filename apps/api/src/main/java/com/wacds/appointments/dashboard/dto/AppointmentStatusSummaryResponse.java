package com.wacds.appointments.dashboard.dto;

public record AppointmentStatusSummaryResponse(
    long total,
    long pending,
    long confirmed,
    long completed,
    long cancelled,
    long noShow
) {
}