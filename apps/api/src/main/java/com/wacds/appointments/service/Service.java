package com.wacds.appointments.service;

import com.wacds.appointments.business.Business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Servicio ofrecido por un Business.
 *
 * =========================================================
 * RESPONSABILIDAD
 * =========================================================
 *
 * Representa un servicio que puede:
 *
 * - mostrarse en el panel Business;
 * - asignarse a profesionales;
 * - participar en disponibilidad;
 * - formar parte del catálogo público;
 * - utilizarse para crear citas.
 *
 * =========================================================
 * RESERVAS
 * =========================================================
 *
 * Para que un Service pueda utilizarse en una nueva reserva:
 *
 * - active = true;
 * - bookingEnabled = true.
 *
 * Aun cumpliendo esas condiciones, la capa de dominio debe
 * comprobar además:
 *
 * - Business activo;
 * - publicBookingEnabled;
 * - Professional compatible;
 * - disponibilidad;
 * - reglas temporales.
 *
 * =========================================================
 * TENANT
 * =========================================================
 *
 * Cada Service pertenece obligatoriamente a un único Business.
 *
 * El slug es único únicamente dentro de ese Business.
 */
@Entity
@Table(
    name = "services",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_services_business_slug",
            columnNames = {
                "business_id",
                "slug"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_services_business_id",
            columnList = "business_id"
        ),
        @Index(
            name = "idx_services_active",
            columnList = "active"
        ),
        @Index(
            name = "idx_services_booking_enabled",
            columnList = "booking_enabled"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_NAME_LENGTH =
        150;

    private static final int MAX_SLUG_LENGTH =
        180;

    private static final int MAX_DESCRIPTION_LENGTH =
        5000;

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    // =========================================================
    // BUSINESS
    // =========================================================

    /**
     * Business propietario del servicio.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "business_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_services_business"
        )
    )
    private Business business;

    // =========================================================
    // IDENTITY
    // =========================================================

    /**
     * Nombre visible del servicio.
     *
     * Ejemplo:
     *
     * Tratamiento facial
     */
    @Column(
        nullable = false,
        length = 150
    )
    private String name;

    /**
     * Identificador legible dentro del Business.
     *
     * Ejemplo:
     *
     * tratamiento-facial
     */
    @Column(
        nullable = false,
        length = 180
    )
    private String slug;

    /**
     * Descripción pública del servicio.
     */
    @Column(
        columnDefinition = "TEXT"
    )
    private String description;

    // =========================================================
    // BOOKING DATA
    // =========================================================

    /**
     * Duración efectiva del servicio en minutos.
     *
     * Debe ser mayor que cero.
     */
    @Column(
        name = "duration_minutes",
        nullable = false
    )
    private Integer durationMinutes;

    /**
     * Precio del servicio.
     *
     * Nunca puede ser negativo.
     */
    @Column(
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal price;

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Estado general del servicio.
     *
     * Un servicio inactivo no debe utilizarse para
     * crear nuevas reservas.
     */
    @Builder.Default
    @Column(
        nullable = false
    )
    private boolean active =
        true;

    /**
     * Determina si el servicio admite nuevas reservas.
     *
     * Permite mantener el servicio activo y visible en el
     * sistema mientras se detienen temporalmente nuevas citas.
     */
    @Builder.Default
    @Column(
        name = "booking_enabled",
        nullable = false
    )
    private boolean bookingEnabled =
        true;

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

    @PrePersist
    void prePersist() {

        normalizeAndValidate();

        OffsetDateTime now =
            OffsetDateTime.now();

        if (
            createdAt == null
        ) {

            createdAt =
                now;
        }

        updatedAt =
            now;
    }

    @PreUpdate
    void preUpdate() {

        normalizeAndValidate();

        updatedAt =
            OffsetDateTime.now();
    }

    // =========================================================
    // DOMAIN VALIDATION
    // =========================================================

    private void normalizeAndValidate() {

        validateBusiness();

        normalizeIdentity();

        validateBookingData();
    }

    // =========================================================
    // BUSINESS
    // =========================================================

    private void validateBusiness() {

        if (
            business == null
        ) {

            throw new IllegalStateException(
                "El servicio debe pertenecer a un negocio."
            );
        }
    }

    // =========================================================
    // IDENTITY
    // =========================================================

    private void normalizeIdentity() {

        // -----------------------------------------------------
        // NAME
        // -----------------------------------------------------

        name =
            requireNonBlank(
                name,
                "El nombre del servicio es obligatorio."
            );

        validateMaxLength(
            name,
            MAX_NAME_LENGTH,
            "El nombre del servicio"
        );

        // -----------------------------------------------------
        // SLUG
        // -----------------------------------------------------

        slug =
            requireNonBlank(
                slug,
                "El slug del servicio es obligatorio."
            )
                .toLowerCase(
                    Locale.ROOT
                );

        validateMaxLength(
            slug,
            MAX_SLUG_LENGTH,
            "El slug del servicio"
        );

        if (
            !slug.matches(
                "^[a-z0-9]+(?:-[a-z0-9]+)*$"
            )
        ) {

            throw new IllegalStateException(
                "El slug del servicio solo puede contener "
                    + "letras minúsculas, números y guiones simples."
            );
        }

        // -----------------------------------------------------
        // DESCRIPTION
        // -----------------------------------------------------

        description =
            normalizeNullable(
                description
            );

        validateMaxLength(
            description,
            MAX_DESCRIPTION_LENGTH,
            "La descripción del servicio"
        );
    }

    // =========================================================
    // BOOKING DATA
    // =========================================================

    private void validateBookingData() {

        if (
            durationMinutes == null
        ) {

            throw new IllegalStateException(
                "La duración del servicio es obligatoria."
            );
        }

        if (
            durationMinutes <= 0
        ) {

            throw new IllegalStateException(
                "La duración del servicio debe ser mayor que 0."
            );
        }

        if (
            price == null
        ) {

            throw new IllegalStateException(
                "El precio del servicio es obligatorio."
            );
        }

        if (
            price.compareTo(
                BigDecimal.ZERO
            ) < 0
        ) {

            throw new IllegalStateException(
                "El precio del servicio no puede ser negativo."
            );
        }
    }

    // =========================================================
    // DOMAIN HELPERS · ACTIVE
    // =========================================================

    public void activate() {

        this.active =
            true;
    }

    public void deactivate() {

        this.active =
            false;
    }

    public void changeActiveStatus(
        boolean active
    ) {

        this.active =
            active;
    }

    // =========================================================
    // DOMAIN HELPERS · BOOKING
    // =========================================================

    public void enableBooking() {

        this.bookingEnabled =
            true;
    }

    public void disableBooking() {

        this.bookingEnabled =
            false;
    }

    public void changeBookingStatus(
        boolean enabled
    ) {

        this.bookingEnabled =
            enabled;
    }

    /**
     * Indica si este servicio puede participar en una nueva
     * reserva.
     *
     * Esta comprobación afecta únicamente al propio Service.
     *
     * La disponibilidad pública completa debe comprobar además
     * Business, Professional, asignación y agenda.
     */
    public boolean canAcceptBookings() {

        return active
            && bookingEnabled;
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private String requireNonBlank(
        String value,
        String message
    ) {

        if (
            value == null
                || value.isBlank()
        ) {

            throw new IllegalStateException(
                message
            );
        }

        return value.trim();
    }

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

    private void validateMaxLength(
        String value,
        int maxLength,
        String fieldName
    ) {

        if (
            value == null
        ) {

            return;
        }

        if (
            value.length()
                > maxLength
        ) {

            throw new IllegalStateException(
                fieldName
                    + " no puede superar los "
                    + maxLength
                    + " caracteres."
            );
        }
    }
}