package com.wacds.appointments.professional;

import com.wacds.appointments.business.Business;
import com.wacds.appointments.professional.dto.ProfessionalRequest;
import com.wacds.appointments.professional.dto.ProfessionalResponse;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ProfessionalMapper {

    // =========================================================
    // REQUEST -> ENTITY
    // =========================================================

    public Professional toEntity(
        ProfessionalRequest request,
        Business business
    ) {

        if (
            request == null
        ) {
            return null;
        }

        boolean active =
            request.active() == null
                || request.active();

        boolean bookingEnabled =
            active
                && (
                    request.bookingEnabled() == null
                        || request.bookingEnabled()
                );

        return Professional.builder()

            // -------------------------------------------------
            // BUSINESS
            // -------------------------------------------------

            .business(
                business
            )

            // -------------------------------------------------
            // IDENTITY
            // -------------------------------------------------

            .firstName(
                normalizeText(
                    request.firstName()
                )
            )

            .lastName(
                normalizeNullableText(
                    request.lastName()
                )
            )

            .displayName(
                resolveDisplayName(
                    request.displayName(),
                    request.firstName(),
                    request.lastName()
                )
            )

            // -------------------------------------------------
            // CONTACT
            // -------------------------------------------------

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

            // -------------------------------------------------
            // PROFILE
            // -------------------------------------------------

            .bio(
                normalizeNullableText(
                    request.bio()
                )
            )

            // -------------------------------------------------
            // STATUS
            // -------------------------------------------------

            .active(
                active
            )

            .bookingEnabled(
                bookingEnabled
            )

            .build();
    }

    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    public ProfessionalResponse toResponse(
        Professional professional
    ) {

        if (
            professional == null
        ) {
            return null;
        }

        if (
            professional.getBusiness() == null
                || professional.getBusiness().getId() == null
        ) {
            throw new IllegalStateException(
                "El profesional no tiene un negocio válido asociado."
            );
        }

        return new ProfessionalResponse(

            professional.getId(),

            professional
                .getBusiness()
                .getId(),

            professional.getFirstName(),

            professional.getLastName(),

            professional.getDisplayName(),

            professional.getEmail(),

            professional.getPhone(),

            professional.getBio(),

            professional.isActive(),

            professional.isBookingEnabled(),

            professional.getCreatedAt(),

            professional.getUpdatedAt()
        );
    }

    // =========================================================
    // DISPLAY NAME
    // =========================================================

    /**
     * El nombre visible sigue esta prioridad:
     *
     * 1. displayName explícito.
     *
     * 2. firstName + lastName.
     *
     * 3. únicamente firstName.
     */
    private String resolveDisplayName(
        String displayName,
        String firstName,
        String lastName
    ) {

        String normalizedDisplayName =
            normalizeNullableText(
                displayName
            );

        if (
            normalizedDisplayName != null
        ) {
            return normalizedDisplayName;
        }

        String normalizedFirstName =
            normalizeText(
                firstName
            );

        String normalizedLastName =
            normalizeNullableText(
                lastName
            );

        if (
            normalizedLastName == null
        ) {
            return normalizedFirstName;
        }

        return (
            normalizedFirstName
                + " "
                + normalizedLastName
        ).trim();
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private String normalizeText(
        String value
    ) {

        if (
            value == null
        ) {
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
            .toLowerCase(
                Locale.ROOT
            );
    }
}