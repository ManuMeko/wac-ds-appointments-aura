
package com.wacds.appointments.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * WAC D.S. - Modelo 01 - Appointments.
 *
 * Entidad principal del negocio o tenant.
 *
 * Cada Business puede disponer de:
 *
 * - Servicios.
 * - Profesionales.
 * - Clientes.
 * - Reservas.
 * - Horarios.
 * - Configuracion de reservas publicas.
 * - Personalizacion visual.
 *
 * MULTITENANCY
 *
 * El identificador businessId permite aislar
 * las operaciones de cada negocio.
 *
 * SEGURIDAD
 *
 * Esta entidad define el estado del negocio.
 *
 * Los servicios deben comprobar los permisos
 * y el aislamiento de cada operacion.
 *
 * CONCURRENCIA
 *
 * Los bloqueos transaccionales de agenda
 * pertenecen al repositorio de reservas.
 *
 * No se adquieren bloqueos desde esta entidad.
 */
@Entity
@Table(
    name = "businesses",
    indexes = {
        @Index(
            name = "idx_businesses_slug",
            columnList = "slug",
            unique = true
        ),
        @Index(
            name = "idx_businesses_email",
            columnList = "email"
        ),
        @Index(
            name = "idx_businesses_active",
            columnList = "active"
        ),
        @Index(
            name = "idx_businesses_city",
            columnList = "city"
        ),
        @Index(
            name = "idx_businesses_country_code",
            columnList = "country_code"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Business {

    // =========================================================
    // DEFAULT VALUES
    // =========================================================

    public static final String DEFAULT_COUNTRY_CODE = "ES";

    public static final String DEFAULT_TIMEZONE = "Europe/Madrid";

    public static final String DEFAULT_LOCALE = "es-ES";

    public static final String DEFAULT_CURRENCY = "EUR";

    // =========================================================
    // LENGTH LIMITS
    // =========================================================

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_SLUG_LENGTH = 180;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;
    private static final int MAX_EMAIL_LENGTH = 180;
    private static final int MAX_PHONE_LENGTH = 40;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_ADDRESS_LENGTH = 180;
    private static final int MAX_POSTAL_CODE_LENGTH = 30;
    private static final int MAX_CITY_LENGTH = 120;
    private static final int MAX_STATE_REGION_LENGTH = 120;
    private static final int MAX_TIMEZONE_LENGTH = 80;
    private static final int MAX_LOCALE_LENGTH = 20;

    // =========================================================
    // VALIDATION PATTERNS
    // =========================================================

    private static final Pattern SLUG_PATTERN =
        Pattern.compile(
            "^[a-z0-9]+(?:-[a-z0-9]+)*$"
        );

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        );

    private static final Pattern COUNTRY_PATTERN =
        Pattern.compile("^[A-Z]{2}$");

    private static final Pattern CURRENCY_PATTERN =
        Pattern.compile("^[A-Z]{3}$");

    private static final Pattern HEX_COLOR_PATTERN =
        Pattern.compile("^#[0-9A-Fa-f]{6}$");

    // =========================================================
    // IDENTITY
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre comercial visible.
     *
     * Ejemplo: Aura Studio.
     */
    @Column(
        nullable = false,
        length = 150
    )
    private String name;

    /**
     * Identificador publico unico del negocio.
     *
     * Ejemplo: aura-studio.
     */
    @Column(
        nullable = false,
        unique = true,
        length = 180
    )
    private String slug;

    // =========================================================
    // DESCRIPTION
    // =========================================================

    /**
     * Descripcion publica del negocio.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    // =========================================================
    // CONTACT
    // =========================================================

    @Column(
        nullable = false,
        length = 180
    )
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(
        name = "website_url",
        length = 500
    )
    private String websiteUrl;

    // =========================================================
    // ADDRESS
    // =========================================================

    @Column(
        name = "address_line_1",
        length = 180
    )
    private String addressLine1;

    @Column(
        name = "address_line_2",
        length = 180
    )
    private String addressLine2;

    @Column(
        name = "postal_code",
        length = 30
    )
    private String postalCode;

    @Column(length = 120)
    private String city;

    @Column(
        name = "state_region",
        length = 120
    )
    private String stateRegion;

    /**
     * Codigo ISO 3166-1 alpha-2.
     */
    @Builder.Default
    @Column(
        name = "country_code",
        nullable = false,
        length = 2
    )
    private String countryCode = DEFAULT_COUNTRY_CODE;

    // =========================================================
    // REGIONAL SETTINGS
    // =========================================================

    /**
     * Zona horaria del negocio.
     *
     * Ejemplo: Europe/Madrid.
     */
    @Builder.Default
    @Column(
        nullable = false,
        length = 80
    )
    private String timezone = DEFAULT_TIMEZONE;

    /**
     * Locale principal.
     *
     * Ejemplo: es-ES.
     */
    @Builder.Default
    @Column(
        nullable = false,
        length = 20
    )
    private String locale = DEFAULT_LOCALE;

    /**
     * Codigo ISO 4217.
     *
     * Ejemplo: EUR.
     */
    @Builder.Default
    @Column(
        nullable = false,
        length = 3
    )
    private String currency = DEFAULT_CURRENCY;

    // =========================================================
    // BRANDING
    // =========================================================

    @Column(
        name = "logo_url",
        length = 500
    )
    private String logoUrl;

    @Column(
        name = "cover_image_url",
        length = 500
    )
    private String coverImageUrl;

    /**
     * Color hexadecimal #RRGGBB.
     */
    @Column(
        name = "primary_color",
        length = 20
    )
    private String primaryColor;

    /**
     * Color hexadecimal #RRGGBB.
     */
    @Column(
        name = "secondary_color",
        length = 20
    )
    private String secondaryColor;

    // =========================================================
    // PUBLIC BOOKING
    // =========================================================

    /**
     * Habilita o deshabilita nuevas reservas publicas.
     *
     * Deshabilitarlo no elimina las reservas existentes.
     */
    @Builder.Default
    @Column(
        name = "public_booking_enabled",
        nullable = false
    )
    private boolean publicBookingEnabled = true;

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Estado operativo global del negocio.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // =========================================================
    // AUDIT
    // =========================================================

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private OffsetDateTime updatedAt;

    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    /**
     * Normaliza la entidad antes de INSERT.
     *
     * Inicializa createdAt y updatedAt.
     */
    @PrePersist
    void prePersist() {

        normalizeAndValidate();

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (
            updatedAt == null
                || updatedAt.isBefore(createdAt)
        ) {
            updatedAt = createdAt;
        }

        if (now.isAfter(updatedAt)) {
            updatedAt = now;
        }
    }

    /**
     * Normaliza la entidad antes de UPDATE.
     *
     * Mantiene createdAt y evita retrocesos
     * en updatedAt.
     */
    @PreUpdate
    void preUpdate() {

        normalizeAndValidate();

        OffsetDateTime now = OffsetDateTime.now();

        if (
            createdAt != null
                && now.isBefore(createdAt)
        ) {
            updatedAt = createdAt;
            return;
        }

        if (
            updatedAt == null
                || now.isAfter(updatedAt)
        ) {
            updatedAt = now;
        }
    }

    // =========================================================
    // NORMALIZATION AND VALIDATION
    // =========================================================

    private void normalizeAndValidate() {

        normalizeIdentity();
        normalizeContact();
        normalizeAddress();
        normalizeRegionalSettings();
        normalizeBranding();
    }

    // =========================================================
    // IDENTITY VALIDATION
    // =========================================================

    private void normalizeIdentity() {

        name = requireNonBlank(
            name,
            "El nombre del negocio es obligatorio."
        );

        validateMaxLength(
            name,
            MAX_NAME_LENGTH,
            "El nombre del negocio"
        );

        slug = requireNonBlank(
            slug,
            "El slug del negocio es obligatorio."
        ).toLowerCase(Locale.ROOT);

        validateMaxLength(
            slug,
            MAX_SLUG_LENGTH,
            "El slug del negocio"
        );

        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalStateException(
                "El slug solo puede contener letras minusculas, "
                    + "numeros y guiones simples."
            );
        }

        description = normalizeNullable(description);

        validateMaxLength(
            description,
            MAX_DESCRIPTION_LENGTH,
            "La descripcion del negocio"
        );
    }

    // =========================================================
    // CONTACT VALIDATION
    // =========================================================

    private void normalizeContact() {

        email = requireNonBlank(
            email,
            "El email del negocio es obligatorio."
        ).toLowerCase(Locale.ROOT);

        validateMaxLength(
            email,
            MAX_EMAIL_LENGTH,
            "El email del negocio"
        );

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalStateException(
                "El email del negocio no tiene un formato valido."
            );
        }

        phone = normalizeNullable(phone);

        validateMaxLength(
            phone,
            MAX_PHONE_LENGTH,
            "El telefono del negocio"
        );

        websiteUrl = normalizeNullable(websiteUrl);

        validateMaxLength(
            websiteUrl,
            MAX_URL_LENGTH,
            "La URL web del negocio"
        );
    }

    // =========================================================
    // ADDRESS VALIDATION
    // =========================================================

    private void normalizeAddress() {

        addressLine1 = normalizeNullable(addressLine1);

        validateMaxLength(
            addressLine1,
            MAX_ADDRESS_LENGTH,
            "La direccion principal"
        );

        addressLine2 = normalizeNullable(addressLine2);

        validateMaxLength(
            addressLine2,
            MAX_ADDRESS_LENGTH,
            "La direccion secundaria"
        );

        postalCode = normalizeNullable(postalCode);

        validateMaxLength(
            postalCode,
            MAX_POSTAL_CODE_LENGTH,
            "El codigo postal"
        );

        city = normalizeNullable(city);

        validateMaxLength(
            city,
            MAX_CITY_LENGTH,
            "La ciudad"
        );

        stateRegion = normalizeNullable(stateRegion);

        validateMaxLength(
            stateRegion,
            MAX_STATE_REGION_LENGTH,
            "La provincia o region"
        );

        if (
            countryCode == null
                || countryCode.isBlank()
        ) {
            countryCode = DEFAULT_COUNTRY_CODE;
        }

        countryCode = countryCode
            .trim()
            .toUpperCase(Locale.ROOT);

        if (
            !COUNTRY_PATTERN.matcher(countryCode).matches()
        ) {
            throw new IllegalStateException(
                "countryCode debe utilizar formato "
                    + "ISO 3166-1 alpha-2."
            );
        }
    }

    // =========================================================
    // REGIONAL SETTINGS VALIDATION
    // =========================================================

    private void normalizeRegionalSettings() {

        normalizeTimezone();
        normalizeLocale();
        normalizeCurrency();
    }

    private void normalizeTimezone() {

        if (
            timezone == null
                || timezone.isBlank()
        ) {
            timezone = DEFAULT_TIMEZONE;
        }

        timezone = timezone.trim();

        validateMaxLength(
            timezone,
            MAX_TIMEZONE_LENGTH,
            "La zona horaria"
        );

        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new IllegalStateException(
                "La zona horaria '"
                    + timezone
                    + "' no es una zona valida.",
                exception
            );
        }
    }

    private void normalizeLocale() {

        if (
            locale == null
                || locale.isBlank()
        ) {
            locale = DEFAULT_LOCALE;
        }

        String normalizedLocale = locale
            .trim()
            .replace('_', '-');

        validateMaxLength(
            normalizedLocale,
            MAX_LOCALE_LENGTH,
            "El locale"
        );

        Locale parsedLocale =
            Locale.forLanguageTag(normalizedLocale);

        if (
            parsedLocale.getLanguage().isBlank()
                || "und".equalsIgnoreCase(
                    parsedLocale.toLanguageTag()
                )
        ) {
            throw new IllegalStateException(
                "El locale '"
                    + normalizedLocale
                    + "' no es valido."
            );
        }

        locale = parsedLocale.toLanguageTag();
    }

    private void normalizeCurrency() {

        if (
            currency == null
                || currency.isBlank()
        ) {
            currency = DEFAULT_CURRENCY;
        }

        currency = currency
            .trim()
            .toUpperCase(Locale.ROOT);

        if (
            !CURRENCY_PATTERN.matcher(currency).matches()
        ) {
            throw new IllegalStateException(
                "currency debe utilizar un codigo ISO 4217 "
                    + "de 3 caracteres."
            );
        }

        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "La moneda '"
                    + currency
                    + "' no corresponde a un codigo "
                    + "ISO 4217 valido.",
                exception
            );
        }
    }

    // =========================================================
    // BRANDING VALIDATION
    // =========================================================

    private void normalizeBranding() {

        logoUrl = normalizeNullable(logoUrl);

        validateMaxLength(
            logoUrl,
            MAX_URL_LENGTH,
            "La URL del logo"
        );

        coverImageUrl = normalizeNullable(coverImageUrl);

        validateMaxLength(
            coverImageUrl,
            MAX_URL_LENGTH,
            "La URL de portada"
        );

        primaryColor = normalizeColor(primaryColor);

        secondaryColor = normalizeColor(secondaryColor);
    }

    // =========================================================
    // DOMAIN HELPERS - STATUS
    // =========================================================

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void changeActiveStatus(boolean active) {
        this.active = active;
    }

    // =========================================================
    // DOMAIN HELPERS - PUBLIC BOOKING
    // =========================================================

    public void enablePublicBooking() {
        this.publicBookingEnabled = true;
    }

    public void disablePublicBooking() {
        this.publicBookingEnabled = false;
    }

    public void changePublicBookingStatus(boolean enabled) {
        this.publicBookingEnabled = enabled;
    }

    /**
     * Indica si el negocio esta activo.
     */
    public boolean isOperational() {
        return active;
    }

    /**
     * Indica si admite nuevas reservas publicas
     * segun los interruptores del propio negocio.
     *
     * No sustituye las comprobaciones de:
     *
     * - Servicio.
     * - Profesional.
     * - Disponibilidad.
     * - Configuracion del negocio.
     */
    public boolean canAcceptPublicBookings() {

        return active && publicBookingEnabled;
    }

    // =========================================================
    // DOMAIN HELPERS - ADDRESS
    // =========================================================

    /**
     * Indica si hay algun dato de direccion.
     */
    public boolean hasAddress() {

        return addressLine1 != null
            || postalCode != null
            || city != null
            || stateRegion != null;
    }

    /**
     * Indica si hay datos basicos de direccion.
     */
    public boolean hasPrimaryAddress() {

        return addressLine1 != null
            && postalCode != null
            && city != null
            && countryCode != null;
    }

    // =========================================================
    // DOMAIN HELPERS - BRANDING
    // =========================================================

    /**
     * Indica si el negocio dispone de branding.
     */
    public boolean hasBranding() {

        return logoUrl != null
            || coverImageUrl != null
            || primaryColor != null
            || secondaryColor != null;
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private String requireNonBlank(
        String value,
        String message
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }

        return value.trim();
    }

    private String normalizeNullable(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeColor(String value) {

        String normalized = normalizeNullable(value);

        if (normalized == null) {
            return null;
        }

        if (
            !HEX_COLOR_PATTERN.matcher(normalized).matches()
        ) {
            throw new IllegalStateException(
                "Los colores deben utilizar "
                    + "formato hexadecimal #RRGGBB."
            );
        }

        return normalized.toUpperCase(Locale.ROOT);
    }

    private void validateMaxLength(
        String value,
        int maxLength,
        String fieldName
    ) {

        if (value == null) {
            return;
        }

        if (value.length() > maxLength) {
            throw new IllegalStateException(
                fieldName
                    + " no puede superar los "
                    + maxLength
                    + " caracteres."
            );
        }
    }
}
