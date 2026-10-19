package com.wacds.appointments.business;

import com.wacds.appointments.business.dto.BusinessRequest;
import com.wacds.appointments.business.dto.BusinessResponse;
import org.springframework.stereotype.Component;

@Component
public class BusinessMapper {

    // =========================================================
    // REQUEST -> ENTITY
    // =========================================================

    public Business toEntity(
        BusinessRequest request
    ) {

        if (
            request == null
        ) {
            return null;
        }

        return Business
            .builder()

            // =================================================
            // IDENTITY
            // =================================================

            .name(
                request.name()
            )

            .slug(
                request.slug()
            )

            .description(
                request.description()
            )

            // =================================================
            // CONTACT
            // =================================================

            .email(
                request.email()
            )

            .phone(
                request.phone()
            )

            .websiteUrl(
                request.websiteUrl()
            )

            // =================================================
            // ADDRESS
            // =================================================

            .addressLine1(
                request.addressLine1()
            )

            .addressLine2(
                request.addressLine2()
            )

            .postalCode(
                request.postalCode()
            )

            .city(
                request.city()
            )

            .stateRegion(
                request.stateRegion()
            )

            .countryCode(
                defaultIfBlank(
                    request.countryCode(),
                    "ES"
                )
            )

            // =================================================
            // REGIONAL SETTINGS
            // =================================================

            .timezone(
                defaultIfBlank(
                    request.timezone(),
                    "Europe/Madrid"
                )
            )

            .locale(
                defaultIfBlank(
                    request.locale(),
                    "es-ES"
                )
            )

            .currency(
                defaultIfBlank(
                    request.currency(),
                    "EUR"
                )
            )

            // =================================================
            // BRANDING
            // =================================================

            .logoUrl(
                request.logoUrl()
            )

            .coverImageUrl(
                request.coverImageUrl()
            )

            .primaryColor(
                request.primaryColor()
            )

            .secondaryColor(
                request.secondaryColor()
            )

            // =================================================
            // PUBLIC BOOKING
            // =================================================

            .publicBookingEnabled(
                request.publicBookingEnabled() == null
                    || request.publicBookingEnabled()
            )

            // =================================================
            // STATUS
            // =================================================

            .active(
                true
            )

            .build();
    }

    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    public BusinessResponse toResponse(
        Business business
    ) {

        if (
            business == null
        ) {
            return null;
        }

        return new BusinessResponse(

            // =================================================
            // IDENTITY
            // =================================================

            business.getId(),

            business.getName(),

            business.getSlug(),

            business.getDescription(),

            // =================================================
            // CONTACT
            // =================================================

            business.getEmail(),

            business.getPhone(),

            business.getWebsiteUrl(),

            // =================================================
            // ADDRESS
            // =================================================

            business.getAddressLine1(),

            business.getAddressLine2(),

            business.getPostalCode(),

            business.getCity(),

            business.getStateRegion(),

            business.getCountryCode(),

            // =================================================
            // REGIONAL SETTINGS
            // =================================================

            business.getTimezone(),

            business.getLocale(),

            business.getCurrency(),

            // =================================================
            // BRANDING
            // =================================================

            business.getLogoUrl(),

            business.getCoverImageUrl(),

            business.getPrimaryColor(),

            business.getSecondaryColor(),

            // =================================================
            // PUBLIC BOOKING
            // =================================================

            business.isPublicBookingEnabled(),

            // =================================================
            // STATUS
            // =================================================

            business.isActive(),

            // =================================================
            // AUDIT
            // =================================================

            business.getCreatedAt(),

            business.getUpdatedAt()
        );
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private String defaultIfBlank(
        String value,
        String defaultValue
    ) {

        if (
            value == null
                || value.isBlank()
        ) {
            return defaultValue;
        }

        return value.trim();
    }
}