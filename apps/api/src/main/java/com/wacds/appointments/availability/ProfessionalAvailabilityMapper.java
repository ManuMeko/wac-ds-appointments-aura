package com.wacds.appointments.availability;

import com.wacds.appointments.availability.dto.ProfessionalAvailabilityRequest;
import com.wacds.appointments.availability.dto.ProfessionalAvailabilityResponse;
import com.wacds.appointments.professional.Professional;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalAvailabilityMapper {

    public ProfessionalAvailability toEntity(
        ProfessionalAvailabilityRequest request,
        Professional professional
    ) {
        if (request == null) {
            return null;
        }

        return ProfessionalAvailability.builder()
            .professional(professional)
            .dayOfWeek(request.dayOfWeek())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .active(
                request.active() == null
                    || request.active()
            )
            .build();
    }

    public ProfessionalAvailabilityResponse toResponse(
        ProfessionalAvailability availability
    ) {
        if (availability == null) {
            return null;
        }

        return new ProfessionalAvailabilityResponse(
            availability.getId(),
            availability.getProfessional().getId(),
            availability.getDayOfWeek(),
            availability.getStartTime(),
            availability.getEndTime(),
            availability.isActive(),
            availability.getCreatedAt(),
            availability.getUpdatedAt()
        );
    }
}