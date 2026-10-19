package com.wacds.appointments.scheduleexception.dto;

import com.wacds.appointments.scheduleexception.ScheduleException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionRequest(

    @NotNull
    LocalDate exceptionDate,

    @NotNull
    ScheduleException.ExceptionType type,

    LocalTime startTime,

    LocalTime endTime,

    @Size(max = 500)
    String reason,

    Boolean active

) {
}