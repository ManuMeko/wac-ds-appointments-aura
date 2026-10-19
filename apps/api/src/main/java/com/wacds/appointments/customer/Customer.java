
package com.wacds.appointments.customer;

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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * WAC D.S. - MODELO 01 - APPOINTMENTS
 *
 * Entidad de dominio que representa un cliente
 * perteneciente a un negocio.
 *
 * RESPONSABILIDADES:
 *
 * - Identificar al cliente.
 * - Asociarlo con un Business.
 * - Almacenar datos de contacto.
 * - Mantener el estado activo.
 * - Normalizar los datos personales.
 * - Conservar auditoria.
 *
 * MULTITENANCY:
 *
 * Cada Customer pertenece a un unico Business.
 *
 * Los clientes de diferentes negocios
 * deben permanecer separados.
 *
 * RESERVAS:
 *
 * PublicBookingService utiliza CustomerService
 * para encontrar o crear un cliente.
 *
 * Appointment mantiene la relacion con Customer.
 *
 * COMPATIBILIDAD:
 *
 * Se conservan:
 *
 * - Tabla customers.
 * - Columnas existentes.
 * - Indices.
 * - Relacion con Business.
 * - Constructor Lombok.
 * - Builder.
 * - Getters y setters.
 * - Metodos de dominio existentes.
 *
 * No requiere migraciones Flyway.
 */
@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(
            name = "idx_customers_business_id",
            columnList = "business_id"
        ),
        @Index(
            name = "idx_customers_business_active",
            columnList = "business_id, active"
        ),
        @Index(
            name = "idx_customers_business_email",
            columnList = "business_id, email"
        ),
        @Index(
            name = "idx_customers_business_phone",
            columnList = "business_id, phone"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_FIRST_NAME_LENGTH = 100;

    private static final int MAX_LAST_NAME_LENGTH = 150;

    private static final int MAX_EMAIL_LENGTH = 180;

    private static final int MAX_PHONE_LENGTH = 40;

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    // =========================================================
    // BUSINESS / TENANT
    // =========================================================

    /**
     * Negocio propietario del cliente.
     *
     * La asociacion es obligatoria.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "business_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_customers_business"
        )
    )
    private Business business;

    // =========================================================
    // CUSTOMER DATA
    // =========================================================

    /**
     * Nombre obligatorio del cliente.
     */
    @Column(
        name = "first_name",
        nullable = false,
        length = 100
    )
    private String firstName;

    /**
     * Apellidos opcionales.
     */
    @Column(
        name = "last_name",
        length = 150
    )
    private String lastName;

    /**
     * Email opcional.
     *
     * Se normaliza en minusculas.
     */
    @Column(
        length = 180
    )
    private String email;

    /**
     * Telefono opcional.
     *
     * Se conservan los caracteres introducidos,
     * eliminando espacios exteriores.
     */
    @Column(
        length = 40
    )
    private String phone;

    /**
     * Observaciones internas opcionales.
     */
    @Column(
        columnDefinition = "TEXT"
    )
    private String notes;

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Estado general del cliente.
     *
     * true por defecto.
     */
    @Builder.Default
    @Column(
        nullable = false
    )
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
     * Normaliza y valida antes de INSERT.
     */
    @PrePersist
    void prePersist() {

        normalizeAndValidate();

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    /**
     * Normaliza y valida antes de UPDATE.
     */
    @PreUpdate
    void preUpdate() {

        normalizeAndValidate();

        updatedAt = OffsetDateTime.now();
    }

    // =========================================================
    // DOMAIN VALIDATION
    // =========================================================

    /**
     * Ejecuta la normalizacion y despues
     * valida los datos persistidos.
     */
    private void normalizeAndValidate() {

        normalize();

        validateBusiness();

        validateCustomerData();
    }

    /**
     * Comprueba que el cliente tenga
     * un negocio propietario.
     *
     * No exige un ID antes del INSERT,
     * para mantener compatibilidad con JPA.
     */
    private void validateBusiness() {

        if (business == null) {
            throw new IllegalStateException(
                "El cliente debe pertenecer a un negocio."
            );
        }
    }

    /**
     * Valida los limites de las columnas.
     *
     * La existencia de email o telefono
     * se exige en PublicBookingRequest y
     * PublicBookingService para las reservas.
     *
     * Aqui se mantiene la posibilidad
     * de clientes de CRM sin contacto,
     * por compatibilidad con el modelo actual.
     */
    private void validateCustomerData() {

        if (
            firstName == null
                || firstName.isBlank()
        ) {
            throw new IllegalStateException(
                "El nombre del cliente es obligatorio."
            );
        }

        validateMaxLength(
            firstName,
            MAX_FIRST_NAME_LENGTH,
            "El nombre del cliente"
        );

        validateMaxLength(
            lastName,
            MAX_LAST_NAME_LENGTH,
            "Los apellidos del cliente"
        );

        validateMaxLength(
            email,
            MAX_EMAIL_LENGTH,
            "El email del cliente"
        );

        validateMaxLength(
            phone,
            MAX_PHONE_LENGTH,
            "El teléfono del cliente"
        );
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    /**
     * Normaliza los campos de texto.
     *
     * Los valores opcionales vacios
     * se convierten en null.
     */
    private void normalize() {

        firstName = normalizeRequiredText(
            firstName
        );

        lastName = normalizeOptionalText(
            lastName
        );

        email = normalizeEmail(
            email
        );

        phone = normalizePhone(
            phone
        );

        notes = normalizeOptionalText(
            notes
        );
    }

    // =========================================================
    // NORMALIZATION HELPERS
    // =========================================================

    private String normalizeRequiredText(
        String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    private String normalizeOptionalText(
        String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    /**
     * Normaliza emails en minusculas.
     *
     * Locale.ROOT evita diferencias
     * segun el idioma del servidor.
     */
    private String normalizeEmail(
        String value
    ) {

        String normalized = normalizeOptionalText(
            value
        );

        if (normalized == null) {
            return null;
        }

        return normalized.toLowerCase(
            Locale.ROOT
        );
    }

    /**
     * Conserva el formato del telefono
     * eliminando espacios exteriores.
     */
    private String normalizePhone(
        String value
    ) {

        return normalizeOptionalText(value);
    }

    // =========================================================
    // VALIDATION HELPERS
    // =========================================================

    /**
     * Comprueba los limites de longitud.
     */
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

    // =========================================================
    // DOMAIN HELPERS - IDENTITY
    // =========================================================

    /**
     * Nombre completo para agenda,
     * respuestas y CRM.
     */
    public String getFullName() {

        String safeFirstName =
            firstName == null
                ? ""
                : firstName.trim();

        String safeLastName =
            lastName == null
                ? ""
                : lastName.trim();

        if (safeLastName.isEmpty()) {
            return safeFirstName;
        }

        if (safeFirstName.isEmpty()) {
            return safeLastName;
        }

        return safeFirstName
            + " "
            + safeLastName;
    }

    // =========================================================
    // DOMAIN HELPERS - CONTACT
    // =========================================================

    /**
     * Devuelve true si existe al menos
     * un canal de contacto.
     */
    public boolean hasContactInformation() {

        return (
            email != null
                && !email.isBlank()
        )
            || (
                phone != null
                    && !phone.isBlank()
            );
    }

    /**
     * Comprueba si el cliente tiene email.
     */
    public boolean hasEmail() {

        return email != null
            && !email.isBlank();
    }

    /**
     * Comprueba si el cliente tiene telefono.
     */
    public boolean hasPhone() {

        return phone != null
            && !phone.isBlank();
    }

    // =========================================================
    // DOMAIN HELPERS - TENANT
    // =========================================================

    /**
     * Identificador del negocio propietario.
     */
    public Long getBusinessId() {

        if (
            business == null
                || business.getId() == null
        ) {
            return null;
        }

        return business.getId();
    }

    /**
     * Comprueba la pertenencia al negocio
     * sin comparar instancias de entidades.
     */
    public boolean belongsToBusiness(
        Long businessId
    ) {

        return businessId != null
            && Objects.equals(
                getBusinessId(),
                businessId
            );
    }

    // =========================================================
    // DOMAIN HELPERS - STATUS
    // =========================================================

    /**
     * Activa el cliente.
     */
    public void activate() {

        this.active = true;
    }

    /**
     * Desactiva el cliente.
     */
    public void deactivate() {

        this.active = false;
    }

    /**
     * Modifica el estado activo.
     */
    public void changeActiveStatus(
        boolean enabled
    ) {

        this.active = enabled;
    }
}
