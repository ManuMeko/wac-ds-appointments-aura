package com.wacds.appointments.appointment;

import com.wacds.appointments.appointment.dto.AppointmentRequest;
import com.wacds.appointments.appointment.dto.AppointmentResponse;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.customer.Customer;
import com.wacds.appointments.professional.Professional;
import com.wacds.appointments.service.Service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Locale;

@Component
public class AppointmentMapper {

    // =========================================================
    // ENTITY
    // =========================================================

    public Appointment toEntity(
        AppointmentRequest request,
        Business business,
        Customer customer,
        Professional professional,
        Service service,
        OffsetDateTime endAt
    ) {

        if (request == null) {
            return null;
        }

        return Appointment.builder()
            .business(
                business
            )
            .customer(
                customer
            )
            .professional(
                professional
            )
            .service(
                service
            )
            .startAt(
                request.startAt()
            )
            .endAt(
                endAt
            )
            .status(
                request.status() == null
                    ? Appointment.AppointmentStatus.PENDING
                    : request.status()
            )
            .notes(
                normalizeNullableText(
                    request.notes()
                )
            )
            .build();
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    public AppointmentResponse toResponse(
        Appointment appointment
    ) {

        if (appointment == null) {
            return null;
        }

        Business business =
            requireBusiness(
                appointment
            );

        Customer customer =
            requireCustomer(
                appointment
            );

        Professional professional =
            requireProfessional(
                appointment
            );

        Service service =
            requireService(
                appointment
            );

        return new AppointmentResponse(

            appointment.getId(),

            business.getId(),

            customer.getId(),

            resolveCustomerName(
                customer
            ),

            normalizeNullableEmail(
                customer.getEmail()
            ),

            normalizeNullableText(
                customer.getPhone()
            ),

            professional.getId(),

            normalizeNullableText(
                professional.getDisplayName()
            ),

            service.getId(),

            normalizeNullableText(
                service.getName()
            ),

            service.getDurationMinutes(),

            service.getPrice(),

            appointment.getStartAt(),

            appointment.getEndAt(),

            normalizeNullableText(
                business.getTimezone()
            ),

            appointment.getStatus(),

            normalizeNullableText(
                appointment.getNotes()
            ),

            appointment.getCreatedAt(),

            appointment.getUpdatedAt()
        );
    }

    // =========================================================
    // RELATION VALIDATION
    // =========================================================

    private Business requireBusiness(
        Appointment appointment
    ) {

        Business business =
            appointment.getBusiness();

        if (
            business == null
                || business.getId() == null
        ) {

            throw new IllegalStateException(
                "La cita no tiene un negocio válido asociado."
            );
        }

        return business;
    }

    private Customer requireCustomer(
        Appointment appointment
    ) {

        Customer customer =
            appointment.getCustomer();

        if (
            customer == null
                || customer.getId() == null
        ) {

            throw new IllegalStateException(
                "La cita no tiene un cliente válido asociado."
            );
        }

        return customer;
    }

    private Professional requireProfessional(
        Appointment appointment
    ) {

        Professional professional =
            appointment.getProfessional();

        if (
            professional == null
                || professional.getId() == null
        ) {

            throw new IllegalStateException(
                "La cita no tiene un profesional válido asociado."
            );
        }

        return professional;
    }

    private Service requireService(
        Appointment appointment
    ) {

        Service service =
            appointment.getService();

        if (
            service == null
                || service.getId() == null
        ) {

            throw new IllegalStateException(
                "La cita no tiene un servicio válido asociado."
            );
        }

        return service;
    }

    // =========================================================
    // CUSTOMER
    // =========================================================

    private String resolveCustomerName(
        Customer customer
    ) {

        if (customer == null) {
            return null;
        }

        String fullName =
            normalizeNullableText(
                customer.getFullName()
            );

        if (fullName != null) {
            return fullName;
        }

        String firstName =
            normalizeNullableText(
                customer.getFirstName()
            );

        String lastName =
            normalizeNullableText(
                customer.getLastName()
            );

        if (
            firstName == null
                && lastName == null
        ) {
            return null;
        }

        if (firstName == null) {
            return lastName;
        }

        if (lastName == null) {
            return firstName;
        }

        return (
            firstName
                + " "
                + lastName
        ).trim();
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private String normalizeNullableEmail(
        String value
    ) {

        String normalized =
            normalizeNullableText(
                value
            );

        if (normalized == null) {
            return null;
        }

        return normalized.toLowerCase(
            Locale.ROOT
        );
    }

    private String normalizeNullableText(
        String value
    ) {

        if (
            value == null
                || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}