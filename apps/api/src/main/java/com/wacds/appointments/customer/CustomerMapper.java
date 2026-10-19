package com.wacds.appointments.customer;

import com.wacds.appointments.business.Business;
import com.wacds.appointments.customer.dto.CustomerRequest;
import com.wacds.appointments.customer.dto.CustomerResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(
        CustomerRequest request,
        Business business
    ) {
        if (request == null) {
            return null;
        }

        return Customer.builder()
            .business(business)
            .firstName(
                normalizeRequiredText(
                    request.firstName()
                )
            )
            .lastName(
                normalizeNullableText(
                    request.lastName()
                )
            )
            .email(
                normalizeEmail(
                    request.email()
                )
            )
            .phone(
                normalizeNullableText(
                    request.phone()
                )
            )
            .notes(
                normalizeNullableText(
                    request.notes()
                )
            )
            .active(
                request.active() == null
                    || request.active()
            )
            .build();
    }

    public CustomerResponse toResponse(
        Customer customer
    ) {
        if (customer == null) {
            return null;
        }

        return new CustomerResponse(
            customer.getId(),
            customer.getBusiness().getId(),
            customer.getFirstName(),
            customer.getLastName(),
            resolveFullName(customer),
            customer.getEmail(),
            customer.getPhone(),
            customer.getNotes(),
            customer.isActive(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }

    private String resolveFullName(
        Customer customer
    ) {
        if (
            customer.getLastName() == null
                || customer.getLastName().isBlank()
        ) {
            return customer.getFirstName();
        }

        return (
            customer.getFirstName()
                + " "
                + customer.getLastName()
        ).trim();
    }

    private String normalizeRequiredText(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
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

    private String normalizeEmail(
        String email
    ) {
        if (
            email == null
                || email.isBlank()
        ) {
            return null;
        }

        return email
            .trim()
            .toLowerCase();
    }
}