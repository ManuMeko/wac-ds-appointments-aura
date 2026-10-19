package com.wacds.appointments.scheduleexception;

import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.scheduleexception.dto.ScheduleExceptionRequest;
import com.wacds.appointments.scheduleexception.dto.ScheduleExceptionResponse;
import org.springframework.stereotype.Component;

@Component
public class ScheduleExceptionMapper {

    public ScheduleException toEntity(
        ScheduleExceptionRequest request,
        Professional professional
    ) {
        if (request == null) {
            return null;
        }

        return ScheduleException.builder()
            .professional(professional)
            .exceptionDate(request.exceptionDate())
            .type(request.type())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .reason(normalizeReason(request.reason()))
            .active(
                request.active() == null
                    || request.active()
            )
            .build();
    }

    public ScheduleExceptionResponse toResponse(
        ScheduleException scheduleException
    ) {
        if (scheduleException == null) {
            return null;
        }

        return new ScheduleExceptionResponse(
            scheduleException.getId(),
            scheduleException.getProfessional().getId(),
            scheduleException.getExceptionDate(),
            scheduleException.getType(),
            scheduleException.getStartTime(),
            scheduleException.getEndTime(),
            scheduleException.getReason(),
            scheduleException.isActive(),
            scheduleException.getCreatedAt(),
            scheduleException.getUpdatedAt()
        );
    }

    private String normalizeReason(
        String reason
    ) {
        if (
            reason == null
                || reason.isBlank()
        ) {
            return null;
        }

        return reason.trim();
    }
}