package com.wacds.appointments.scheduleexception.dto;

import com.wacds.appointments.scheduleexception.ScheduleException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record ScheduleExceptionResponse(

    Long id,

    Long professionalId,

    LocalDate exceptionDate,

    ScheduleException.ExceptionType type,

    LocalTime startTime,

    LocalTime endTime,

    String reason,

    boolean active,

    OffsetDateTime createdAt,

    OffsetDateTime updatedAt

) {
}