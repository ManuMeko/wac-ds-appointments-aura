package com.wacds.appointments.professionalservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProfessionalServiceAssignmentResponse(

    Long professionalId,

    Long serviceId,

    String serviceName,

    String serviceSlug,

    Integer durationMinutes,

    BigDecimal price,

    boolean serviceActive,

    boolean serviceBookingEnabled,

    OffsetDateTime assignedAt

) {
}