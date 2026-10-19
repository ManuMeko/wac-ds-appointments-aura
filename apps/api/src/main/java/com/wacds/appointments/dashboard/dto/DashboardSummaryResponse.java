package com.wacds.appointments.dashboard.dto;

import com.wacds.appointments.appointment.dto.AppointmentResponse;

import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryResponse(

    Long businessId,

    LocalDate date,

    String timezone,

    long todayAppointments,

    long upcomingAppointments,

    AppointmentStatusSummaryResponse statusSummary,

    List<AppointmentResponse> today,

    List<AppointmentResponse> upcoming

) {
}