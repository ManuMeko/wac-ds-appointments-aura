
package com.wacds.appointments.professional;

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
import java.util.regex.Pattern;

/**
 * WAC D.S. - Modelo 01 - Appointments
 *
 * Profesional perteneciente a un Business.
 *
 * RESPONSABILIDAD
 *
 * Representa una persona o recurso profesional que puede:
 *
 * - aparecer en el panel del negocio;
 * - tener servicios asignados;
 * - disponer de horarios semanales;
 * - tener excepciones de agenda;
 * - participar en el calculo de disponibilidad;
 * - recibir reservas.
 *
 * RESERVAS
 *
 * Para aceptar nuevas reservas debe cumplir:
 *
 * - active = true;
 * - bookingEnabled = true.
 *
 * La capa de servicios debe comprobar ademas:
 *
 * - negocio activo;
 * - reservas publicas habilitadas;
 * - servicio activo y compatible;
 * - asociacion profesional-servicio;
 * - horarios y excepciones;
 * - disponibilidad efectiva;
 * - ausencia de solapamientos.
 *
 * MULTITENANCY
 *
 * Cada profesional pertenece obligatoriamente
 * a un negocio concreto.
 *
 * CONCURRENCIA
 *
 * La coordinacion de escrituras de agenda se realiza
 * mediante el bloqueo de la fila del profesional
 * implementado en AppointmentRepository.
 *
 * Este modelo no adquiere bloqueos por si mismo.
 */
@Entity
@Table(
    name = "professionals",
    indexes = {
        @Index(
            name = "idx_professionals_business_id",
            columnList = "business_id"
        ),
        @Index(
            name = "idx_professionals_business_active",
            columnList = "business_id, active"
        ),
        @Index(
            name = "idx_professionals_business_booking_enabled",
            columnList = "business_id, booking_enabled"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Professional {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_FIRST_NAME_LENGTH = 100;

    private static final int MAX_LAST_NAME_LENGTH = 150;

    private static final int MAX_DISPLAY_NAME_LENGTH = 180;

    private static final int MAX_EMAIL_LENGTH = 180;

    private static final int MAX_PHONE_LENGTH = 40;

    private static final int MAX_BIO_LENGTH = 5000;

    /**
     * Comprobacion basica de formato de correo.
     *
     * No pretende implementar todas las reglas RFC,
     * pero rechaza espacios, ausencia de dominio
     * y formatos manifiestamente incorrectos.
     */
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        );

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================
    // BUSINESS
    // =========================================================

    /**
     * Negocio propietario del profesional.
     *
     * La relacion es obligatoria.
     *
     * Las consultas de agenda deben comprobar
     * conjuntamente businessId y professionalId.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "business_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_professionals_business"
        )
    )
    private Business business;

    // =========================================================
    // IDENTITY
    // =========================================================

    /**
     * Nombre del profesional.
     */
    @Column(
        name = "first_name",
        nullable = false,
        length = 100
    )
    private String firstName;

    /**
     * Apellidos del profesional.
     *
     * Campo opcional.
     */
    @Column(
        name = "last_name",
        length = 150
    )
    private String lastName;

    /**
     * Nombre publico utilizado por el frontend.
     *
     * Ejemplos:
     *
     * Laura Martin
     * Lucia - Especialista facial
     *
     * Campo obligatorio.
     */
    @Column(
        name = "display_name",
        nullable = false,
        length = 180
    )
    private String displayName;

    // =========================================================
    // CONTACT
    // =========================================================

    /**
     * Correo profesional opcional.
     *
     * Se normaliza a minusculas antes de persistir.
     */
    @Column(length = 180)
    private String email;

    /**
     * Telefono profesional opcional.
     */
    @Column(length = 40)
    private String phone;

    // =========================================================
    // PUBLIC PROFILE
    // =========================================================

    /**
     * Descripcion publica del profesional.
     */
    @Column(columnDefinition = "TEXT")
    private String bio;

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Estado general del profesional.
     *
     * Un profesional inactivo no debe recibir
     * nuevas reservas.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Permite pausar nuevas reservas sin desactivar
     * completamente al profesional.
     */
    @Builder.Default
    @Column(
        name = "booking_enabled",
        nullable = false
    )
    private boolean bookingEnabled = true;

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
     * Normaliza y valida los campos antes de INSERT.
     *
     * Inicializa las fechas de auditoria.
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
     * Normaliza y valida antes de UPDATE.
     *
     * Mantiene createdAt sin modificar.
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
    // DOMAIN VALIDATION
    // =========================================================

    /**
     * Ejecuta la validacion completa de los campos
     * antes de persistir o actualizar.
     */
    private void normalizeAndValidate() {

        validateBusiness();

        normalizeIdentity();

        normalizeContact();

        normalizeBio();
    }

    // =========================================================
    // BUSINESS VALIDATION
    // =========================================================

    private void validateBusiness() {

        if (business == null) {
            throw new IllegalStateException(
                "El profesional debe pertenecer a un negocio."
            );
        }
    }

    // =========================================================
    // IDENTITY VALIDATION
    // =========================================================

    private void normalizeIdentity() {

        // -----------------------------------------------------
        // FIRST NAME
        // -----------------------------------------------------

        firstName = requireNonBlank(
            firstName,
            "El nombre del profesional es obligatorio."
        );

        validateMaxLength(
            firstName,
            MAX_FIRST_NAME_LENGTH,
            "El nombre del profesional"
        );

        // -----------------------------------------------------
        // LAST NAME
        // -----------------------------------------------------

        lastName = normalizeNullable(lastName);

        validateMaxLength(
            lastName,
            MAX_LAST_NAME_LENGTH,
            "Los apellidos del profesional"
        );

        // -----------------------------------------------------
        // DISPLAY NAME
        // -----------------------------------------------------

        displayName = requireNonBlank(
            displayName,
            "El nombre visible del profesional es obligatorio."
        );

        validateMaxLength(
            displayName,
            MAX_DISPLAY_NAME_LENGTH,
            "El nombre visible del profesional"
        );
    }

    // =========================================================
    // CONTACT VALIDATION
    // =========================================================

    private void normalizeContact() {

        // -----------------------------------------------------
        // EMAIL
        // -----------------------------------------------------

        email = normalizeNullable(email);

        if (email != null) {

            email = email.toLowerCase(Locale.ROOT);

            validateMaxLength(
                email,
                MAX_EMAIL_LENGTH,
                "El email del profesional"
            );

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalStateException(
                    "El email del profesional no tiene "
                        + "un formato valido."
                );
            }
        }

        // -----------------------------------------------------
        // PHONE
        // -----------------------------------------------------

        phone = normalizeNullable(phone);

        validateMaxLength(
            phone,
            MAX_PHONE_LENGTH,
            "El telefono del profesional"
        );
    }

    // =========================================================
    // BIO VALIDATION
    // =========================================================

    private void normalizeBio() {

        bio = normalizeNullable(bio);

        validateMaxLength(
            bio,
            MAX_BIO_LENGTH,
            "La biografia del profesional"
        );
    }

    // =========================================================
    // DOMAIN HELPERS - ACTIVE
    // =========================================================

    /**
     * Activa el profesional.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Desactiva el profesional.
     *
     * No elimina sus reservas existentes.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Cambia el estado general del profesional.
     */
    public void changeActiveStatus(boolean active) {
        this.active = active;
    }

    // =========================================================
    // DOMAIN HELPERS - BOOKING
    // =========================================================

    /**
     * Habilita nuevas reservas.
     */
    public void enableBooking() {
        this.bookingEnabled = true;
    }

    /**
     * Deshabilita nuevas reservas.
     *
     * No cancela las reservas existentes.
     */
    public void disableBooking() {
        this.bookingEnabled = false;
    }

    /**
     * Modifica la disponibilidad del profesional
     * para aceptar nuevas reservas.
     */
    public void changeBookingStatus(boolean enabled) {
        this.bookingEnabled = enabled;
    }

    /**
     * Indica si el profesional puede aceptar reservas
     * segun sus propios indicadores.
     *
     * No sustituye la validacion completa del negocio,
     * servicio, horario y solapamientos.
     */
    public boolean canAcceptBookings() {

        return active && bookingEnabled;
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Exige un texto no vacio y devuelve
     * el valor sin espacios exteriores.
     */
    private String requireNonBlank(
        String value,
        String message
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }

        return value.trim();
    }

    /**
     * Convierte textos vacios o con solo espacios
     * a null.
     */
    private String normalizeNullable(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Valida la longitud maxima de un texto.
     *
     * Los campos opcionales pueden ser null.
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
}
