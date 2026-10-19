package com.wacds.appointments.business;

import com.wacds.appointments.business.dto.BusinessRequest;
import com.wacds.appointments.business.dto.BusinessResponse;
import com.wacds.appointments.business.dto.BusinessUpdateRequest;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Currency;
import java.util.IllformedLocaleException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BusinessService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessRepository businessRepository;

    private final BusinessMapper businessMapper;

    // =========================================================
    // CREATE BUSINESS
    // =========================================================

    /**
     * Crea un nuevo Business.
     *
     * Esta operación representa provisioning estructural
     * del tenant.
     *
     * Actualmente se conserva como operación interna del
     * dominio y no debe exponerse públicamente sin una
     * autorización específica de plataforma.
     */
    @Transactional
    public BusinessResponse create(
        BusinessRequest request
    ) {

        validateCreateRequest(
            request
        );

        String normalizedSlug =
            normalizeSlug(
                request.slug()
            );

        String normalizedEmail =
            normalizeEmail(
                request.email()
            );

        // -----------------------------------------------------
        // SLUG UNIQUENESS
        // -----------------------------------------------------

        if (
            businessRepository
                .existsBySlugIgnoreCase(
                    normalizedSlug
                )
        ) {
            throw new ConflictException(
                "Ya existe un negocio con el slug: "
                    + normalizedSlug
            );
        }

        // -----------------------------------------------------
        // EMAIL UNIQUENESS
        // -----------------------------------------------------

        if (
            businessRepository
                .existsByEmailIgnoreCase(
                    normalizedEmail
                )
        ) {
            throw new ConflictException(
                "Ya existe un negocio con el email: "
                    + normalizedEmail
            );
        }

        Business business =
            businessMapper.toEntity(
                request
            );

        if (
            business == null
        ) {
            throw new IllegalStateException(
                "BusinessMapper no ha generado "
                    + "una entidad Business válida."
            );
        }

        business.setSlug(
            normalizedSlug
        );

        business.setEmail(
            normalizedEmail
        );

        Business savedBusiness =
            businessRepository.save(
                business
            );

        return businessMapper.toResponse(
            savedBusiness
        );
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    /**
     * Busca un Business por identificador.
     *
     * Este método no realiza autorización multi-tenant.
     *
     * Los controllers privados deben validar previamente
     * BusinessMembershipService.
     */
    @Transactional(readOnly = true)
    public BusinessResponse findById(
        Long id
    ) {

        Business business =
            findEntityById(
                id
            );

        return businessMapper.toResponse(
            business
        );
    }

    // =========================================================
    // FIND BY SLUG
    // =========================================================

    @Transactional(readOnly = true)
    public BusinessResponse findBySlug(
        String slug
    ) {

        String normalizedSlug =
            normalizeSlug(
                slug
            );

        Business business =
            businessRepository
                .findBySlugIgnoreCase(
                    normalizedSlug
                )
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "No existe ningún negocio con slug: "
                                + normalizedSlug
                        )
                );

        return businessMapper.toResponse(
            business
        );
    }

    // =========================================================
    // UPDATE BUSINESS
    // =========================================================

    /**
     * Actualiza parcialmente el perfil de un Business.
     *
     * Semántica PATCH:
     *
     * null:
     * - no modificar.
     *
     * valor:
     * - modificar.
     *
     * Para Strings opcionales:
     *
     * "":
     * - elimina el valor.
     *
     * Los campos estructuralmente obligatorios no pueden
     * quedar vacíos:
     *
     * - name
     * - slug
     * - email
     * - countryCode
     * - timezone
     * - locale
     * - currency
     *
     * El campo active no forma parte de BusinessUpdateRequest,
     * por lo que esta operación nunca activa o desactiva
     * administrativamente el tenant.
     */
    @Transactional
    public BusinessResponse update(
        Long businessId,
        BusinessUpdateRequest request
    ) {

        validateBusinessId(
            businessId
        );

        validateUpdateRequest(
            request
        );

        Business business =
            findEntityById(
                businessId
            );

        // =====================================================
        // IDENTITY
        // =====================================================

        if (
            request.name() != null
        ) {
            business.setName(
                requireNonBlank(
                    request.name(),
                    "El nombre del negocio no puede estar vacío."
                )
            );
        }

        if (
            request.slug() != null
        ) {

            String normalizedSlug =
                normalizeSlug(
                    request.slug()
                );

            if (
                businessRepository
                    .existsBySlugIgnoreCaseAndIdNot(
                        normalizedSlug,
                        businessId
                    )
            ) {
                throw new ConflictException(
                    "Ya existe otro negocio con el slug: "
                        + normalizedSlug
                );
            }

            business.setSlug(
                normalizedSlug
            );
        }

        if (
            request.description() != null
        ) {
            business.setDescription(
                normalizeNullable(
                    request.description()
                )
            );
        }

        // =====================================================
        // CONTACT
        // =====================================================

        if (
            request.email() != null
        ) {

            String normalizedEmail =
                normalizeEmail(
                    request.email()
                );

            if (
                businessRepository
                    .existsByEmailIgnoreCaseAndIdNot(
                        normalizedEmail,
                        businessId
                    )
            ) {
                throw new ConflictException(
                    "Ya existe otro negocio con el email: "
                        + normalizedEmail
                );
            }

            business.setEmail(
                normalizedEmail
            );
        }

        if (
            request.phone() != null
        ) {
            business.setPhone(
                normalizeNullable(
                    request.phone()
                )
            );
        }

        if (
            request.websiteUrl() != null
        ) {
            business.setWebsiteUrl(
                normalizeNullable(
                    request.websiteUrl()
                )
            );
        }

        // =====================================================
        // ADDRESS
        // =====================================================

        if (
            request.addressLine1() != null
        ) {
            business.setAddressLine1(
                normalizeNullable(
                    request.addressLine1()
                )
            );
        }

        if (
            request.addressLine2() != null
        ) {
            business.setAddressLine2(
                normalizeNullable(
                    request.addressLine2()
                )
            );
        }

        if (
            request.postalCode() != null
        ) {
            business.setPostalCode(
                normalizeNullable(
                    request.postalCode()
                )
            );
        }

        if (
            request.city() != null
        ) {
            business.setCity(
                normalizeNullable(
                    request.city()
                )
            );
        }

        if (
            request.stateRegion() != null
        ) {
            business.setStateRegion(
                normalizeNullable(
                    request.stateRegion()
                )
            );
        }

        if (
            request.countryCode() != null
        ) {

            String countryCode =
                normalizeCountryCode(
                    request.countryCode()
                );

            business.setCountryCode(
                countryCode
            );
        }

        // =====================================================
        // REGIONAL SETTINGS
        // =====================================================

        if (
            request.timezone() != null
        ) {

            String timezone =
                normalizeTimezone(
                    request.timezone()
                );

            business.setTimezone(
                timezone
            );
        }

        if (
            request.locale() != null
        ) {

            String locale =
                normalizeLocale(
                    request.locale()
                );

            business.setLocale(
                locale
            );
        }

        if (
            request.currency() != null
        ) {

            String currency =
                normalizeCurrency(
                    request.currency()
                );

            business.setCurrency(
                currency
            );
        }

        // =====================================================
        // BRANDING
        // =====================================================

        if (
            request.logoUrl() != null
        ) {
            business.setLogoUrl(
                normalizeNullable(
                    request.logoUrl()
                )
            );
        }

        if (
            request.coverImageUrl() != null
        ) {
            business.setCoverImageUrl(
                normalizeNullable(
                    request.coverImageUrl()
                )
            );
        }

        if (
            request.primaryColor() != null
        ) {
            business.setPrimaryColor(
                normalizeHexColor(
                    request.primaryColor()
                )
            );
        }

        if (
            request.secondaryColor() != null
        ) {
            business.setSecondaryColor(
                normalizeHexColor(
                    request.secondaryColor()
                )
            );
        }

        // =====================================================
        // PUBLIC BOOKING
        // =====================================================

        if (
            request.publicBookingEnabled() != null
        ) {
            business.changePublicBookingStatus(
                request.publicBookingEnabled()
            );
        }

        // =====================================================
        // SAVE
        // =====================================================

        Business savedBusiness =
            businessRepository.save(
                business
            );

        return businessMapper.toResponse(
            savedBusiness
        );
    }

    // =========================================================
    // INTERNAL ENTITY LOOKUP
    // =========================================================

    /**
     * Recupera la entidad Business.
     *
     * No realiza autorización por sí misma.
     */
    @Transactional(readOnly = true)
    public Business findEntityById(
        Long id
    ) {

        validateBusinessId(
            id
        );

        return businessRepository
            .findById(
                id
            )
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No existe ningún negocio con id: "
                            + id
                    )
            );
    }

    // =========================================================
    // EXISTS
    // =========================================================

    @Transactional(readOnly = true)
    public boolean existsById(
        Long id
    ) {

        if (
            id == null
                || id <= 0
        ) {
            return false;
        }

        return businessRepository
            .existsById(
                id
            );
    }

    @Transactional(readOnly = true)
    public boolean existsBySlug(
        String slug
    ) {

        if (
            slug == null
                || slug.isBlank()
        ) {
            return false;
        }

        return businessRepository
            .existsBySlugIgnoreCase(
                normalizeSlug(
                    slug
                )
            );
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(
        String email
    ) {

        if (
            email == null
                || email.isBlank()
        ) {
            return false;
        }

        return businessRepository
            .existsByEmailIgnoreCase(
                normalizeEmail(
                    email
                )
            );
    }

    // =========================================================
    // UNIQUE VALIDATION · UPDATE
    // =========================================================

    @Transactional(readOnly = true)
    public boolean slugBelongsToAnotherBusiness(
        String slug,
        Long currentBusinessId
    ) {

        validateBusinessId(
            currentBusinessId
        );

        String normalizedSlug =
            normalizeSlug(
                slug
            );

        return businessRepository
            .existsBySlugIgnoreCaseAndIdNot(
                normalizedSlug,
                currentBusinessId
            );
    }

    @Transactional(readOnly = true)
    public boolean emailBelongsToAnotherBusiness(
        String email,
        Long currentBusinessId
    ) {

        validateBusinessId(
            currentBusinessId
        );

        String normalizedEmail =
            normalizeEmail(
                email
            );

        return businessRepository
            .existsByEmailIgnoreCaseAndIdNot(
                normalizedEmail,
                currentBusinessId
            );
    }

    // =========================================================
    // CREATE REQUEST VALIDATION
    // =========================================================

    private void validateCreateRequest(
        BusinessRequest request
    ) {

        if (
            request == null
        ) {
            throw new IllegalArgumentException(
                "La solicitud de creación del negocio es obligatoria."
            );
        }

        if (
            request.name() == null
                || request.name().isBlank()
        ) {
            throw new IllegalArgumentException(
                "El nombre del negocio es obligatorio."
            );
        }

        if (
            request.slug() == null
                || request.slug().isBlank()
        ) {
            throw new IllegalArgumentException(
                "El slug del negocio es obligatorio."
            );
        }

        if (
            request.email() == null
                || request.email().isBlank()
        ) {
            throw new IllegalArgumentException(
                "El email del negocio es obligatorio."
            );
        }
    }

    // =========================================================
    // UPDATE REQUEST VALIDATION
    // =========================================================

    private void validateUpdateRequest(
        BusinessUpdateRequest request
    ) {

        if (
            request == null
        ) {
            throw new IllegalArgumentException(
                "La solicitud de actualización "
                    + "del negocio es obligatoria."
            );
        }

        /*
         * No exigimos al menos un campo.
         *
         * PATCH {}
         *
         * es idempotente y devuelve simplemente
         * el estado actual.
         */
    }

    // =========================================================
    // BUSINESS ID VALIDATION
    // =========================================================

    private void validateBusinessId(
        Long id
    ) {

        if (
            id == null
        ) {
            throw new IllegalArgumentException(
                "businessId es obligatorio."
            );
        }

        if (
            id <= 0
        ) {
            throw new IllegalArgumentException(
                "businessId debe ser mayor que 0."
            );
        }
    }

    // =========================================================
    // SLUG NORMALIZATION
    // =========================================================

    private String normalizeSlug(
        String slug
    ) {

        if (
            slug == null
                || slug.isBlank()
        ) {
            throw new IllegalArgumentException(
                "El slug del negocio es obligatorio."
            );
        }

        String normalized =
            slug
                .trim()
                .toLowerCase(
                    Locale.ROOT
                );

        if (
            normalized.length() > 180
        ) {
            throw new IllegalArgumentException(
                "El slug no puede superar los 180 caracteres."
            );
        }

        return normalized;
    }

    // =========================================================
    // EMAIL NORMALIZATION
    // =========================================================

    private String normalizeEmail(
        String email
    ) {

        if (
            email == null
                || email.isBlank()
        ) {
            throw new IllegalArgumentException(
                "El email del negocio es obligatorio."
            );
        }

        String normalized =
            email
                .trim()
                .toLowerCase(
                    Locale.ROOT
                );

        if (
            normalized.length() > 180
        ) {
            throw new IllegalArgumentException(
                "El email no puede superar los 180 caracteres."
            );
        }

        return normalized;
    }

    // =========================================================
    // COUNTRY CODE
    // =========================================================

    private String normalizeCountryCode(
        String value
    ) {

        String normalized =
            requireNonBlank(
                value,
                "El código de país no puede estar vacío."
            )
                .toUpperCase(
                    Locale.ROOT
                );

        if (
            !normalized.matches(
                "^[A-Z]{2}$"
            )
        ) {
            throw new IllegalArgumentException(
                "El código de país debe contener exactamente 2 letras."
            );
        }

        return normalized;
    }

    // =========================================================
    // TIMEZONE
    // =========================================================

    private String normalizeTimezone(
        String value
    ) {

        String normalized =
            requireNonBlank(
                value,
                "La zona horaria no puede estar vacía."
            );

        try {
            return ZoneId
                .of(
                    normalized
                )
                .getId();
        } catch (
            DateTimeException exception
        ) {
            throw new IllegalArgumentException(
                "La zona horaria '"
                    + normalized
                    + "' no es válida."
            );
        }
    }

    // =========================================================
    // LOCALE
    // =========================================================

    private String normalizeLocale(
        String value
    ) {

        String normalized =
            requireNonBlank(
                value,
                "El locale no puede estar vacío."
            );

        /*
         * Java utiliza normalmente etiquetas BCP 47.
         *
         * Ejemplos válidos:
         *
         * es
         * es-ES
         * en-US
         */
        String languageTag =
            normalized
                .replace(
                    '_',
                    '-'
                );

        try {

            Locale locale =
                new Locale.Builder()
                    .setLanguageTag(
                        languageTag
                    )
                    .build();

            if (
                locale.getLanguage() == null
                    || locale.getLanguage().isBlank()
            ) {
                throw new IllegalArgumentException(
                    "El locale '"
                        + normalized
                        + "' no es válido."
                );
            }

            return locale.toLanguageTag();

        } catch (
            IllformedLocaleException exception
        ) {
            throw new IllegalArgumentException(
                "El locale '"
                    + normalized
                    + "' no es válido."
            );
        }
    }

    // =========================================================
    // CURRENCY
    // =========================================================

    private String normalizeCurrency(
        String value
    ) {

        String normalized =
            requireNonBlank(
                value,
                "La moneda no puede estar vacía."
            )
                .toUpperCase(
                    Locale.ROOT
                );

        if (
            !normalized.matches(
                "^[A-Z]{3}$"
            )
        ) {
            throw new IllegalArgumentException(
                "La moneda debe contener exactamente 3 letras."
            );
        }

        try {

            Currency currency =
                Currency.getInstance(
                    normalized
                );

            return currency.getCurrencyCode();

        } catch (
            IllegalArgumentException exception
        ) {
            throw new IllegalArgumentException(
                "La moneda '"
                    + normalized
                    + "' no es un código ISO 4217 válido."
            );
        }
    }

    // =========================================================
    // HEX COLOR
    // =========================================================

    private String normalizeHexColor(
        String value
    ) {

        String normalized =
            normalizeNullable(
                value
            );

        if (
            normalized == null
        ) {
            return null;
        }

        normalized =
            normalized.toUpperCase(
                Locale.ROOT
            );

        if (
            !normalized.matches(
                "^#[0-9A-F]{6}$"
            )
        ) {
            throw new IllegalArgumentException(
                "El color debe utilizar formato hexadecimal #RRGGBB."
            );
        }

        return normalized;
    }

    // =========================================================
    // REQUIRED STRING
    // =========================================================

    private String requireNonBlank(
        String value,
        String message
    ) {

        if (
            value == null
                || value.isBlank()
        ) {
            throw new IllegalArgumentException(
                message
            );
        }

        return value.trim();
    }

    // =========================================================
    // NULLABLE STRING
    // =========================================================

    /**
     * Para campos opcionales:
     *
     * null:
     * - significa "no actualizar" antes de llegar aquí.
     *
     * "":
     * - significa eliminar el valor.
     */
    private String normalizeNullable(
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