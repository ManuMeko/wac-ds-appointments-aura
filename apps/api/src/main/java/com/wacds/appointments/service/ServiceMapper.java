package com.wacds.appointments.service;

import com.wacds.appointments.business.Business;
import com.wacds.appointments.service.dto.ServiceRequest;
import com.wacds.appointments.service.dto.ServiceResponse;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {

    public Service toEntity(
        ServiceRequest request,
        Business business
    ) {
        if (request == null) {
            return null;
        }

        return Service.builder()
            .business(business)
            .name(request.name())
            .slug(request.slug())
            .description(request.description())
            .durationMinutes(request.durationMinutes())
            .price(request.price())
            .active(true)
            .bookingEnabled(true)
            .build();
    }

    public ServiceResponse toResponse(Service service) {
        if (service == null) {
            return null;
        }

        return new ServiceResponse(
            service.getId(),
            service.getBusiness().getId(),
            service.getName(),
            service.getSlug(),
            service.getDescription(),
            service.getDurationMinutes(),
            service.getPrice(),
            service.isActive(),
            service.isBookingEnabled(),
            service.getCreatedAt(),
            service.getUpdatedAt()
        );
    }
}