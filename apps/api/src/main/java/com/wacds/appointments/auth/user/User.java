package com.wacds.appointments.auth.user;

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

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(
            name = "ix_users_active",
            columnList = "active"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    // =========================================================
    // AUTHENTICATION
    // =========================================================

    @Column(
        name = "email",
        nullable = false,
        length = 255
    )
    private String email;

    /**
     * Nunca debe contener la contraseña en texto plano.
     *
     * Debe almacenar exclusivamente un hash generado
     * mediante PasswordEncoder.
     */
    @Column(
        name = "password_hash",
        nullable = false,
        length = 255
    )
    private String passwordHash;

    // =========================================================
    // PROFILE
    // =========================================================

    @Column(
        name = "first_name",
        nullable = false,
        length = 100
    )
    private String firstName;

    @Column(
        name = "last_name",
        nullable = false,
        length = 150
    )
    private String lastName;

    // =========================================================
    // ACCOUNT STATUS
    // =========================================================

    @Column(
        name = "active",
        nullable = false
    )
    @Builder.Default
    private boolean active = true;

    @Column(
        name = "email_verified",
        nullable = false
    )
    @Builder.Default
    private boolean emailVerified = false;

    // =========================================================
    // LOGIN INFORMATION
    // =========================================================

    @Column(
        name = "last_login_at"
    )
    private OffsetDateTime lastLoginAt;

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
    // JPA LIFECYCLE · CREATE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        validateAndNormalize();

        OffsetDateTime now =
            OffsetDateTime.now();

        if (createdAt == null) {
            createdAt =
                now;
        }

        if (updatedAt == null) {
            updatedAt =
                now;
        }
    }

    // =========================================================
    // JPA LIFECYCLE · UPDATE
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        validateAndNormalize();

        updatedAt =
            OffsetDateTime.now();
    }

    // =========================================================
    // DOMAIN VALIDATION
    // =========================================================

    private void validateAndNormalize() {

        validateEmail();

        validatePasswordHash();

        validateFirstName();

        validateLastName();

        email =
            normalizeEmail(
                email
            );

        firstName =
            firstName.trim();

        lastName =
            lastName.trim();
    }

    private void validateEmail() {

        if (
            email == null
                || email.isBlank()
        ) {
            throw new IllegalStateException(
                "El usuario debe tener un email."
            );
        }

        String normalized =
            email.trim();

        if (
            normalized.length()
                > 255
        ) {
            throw new IllegalStateException(
                "El email del usuario supera los 255 caracteres."
            );
        }
    }

    private void validatePasswordHash() {

        if (
            passwordHash == null
                || passwordHash.isBlank()
        ) {
            throw new IllegalStateException(
                "El usuario debe tener un passwordHash."
            );
        }

        if (
            passwordHash.length()
                > 255
        ) {
            throw new IllegalStateException(
                "El passwordHash supera los 255 caracteres."
            );
        }
    }

    private void validateFirstName() {

        if (
            firstName == null
                || firstName.isBlank()
        ) {
            throw new IllegalStateException(
                "El nombre del usuario es obligatorio."
            );
        }

        String normalized =
            firstName.trim();

        if (
            normalized.length()
                > 100
        ) {
            throw new IllegalStateException(
                "El nombre del usuario supera los 100 caracteres."
            );
        }
    }

    private void validateLastName() {

        if (
            lastName == null
                || lastName.isBlank()
        ) {
            throw new IllegalStateException(
                "Los apellidos del usuario son obligatorios."
            );
        }

        String normalized =
            lastName.trim();

        if (
            normalized.length()
                > 150
        ) {
            throw new IllegalStateException(
                "Los apellidos del usuario superan los 150 caracteres."
            );
        }
    }

    // =========================================================
    // DOMAIN HELPERS · PROFILE
    // =========================================================

    public String getFullName() {

        String first =
            firstName == null
                ? ""
                : firstName.trim();

        String last =
            lastName == null
                ? ""
                : lastName.trim();

        return (
            first
                + " "
                + last
        ).trim();
    }

    public boolean hasSameEmail(
        String candidateEmail
    ) {

        if (
            candidateEmail == null
                || candidateEmail.isBlank()
                || email == null
                || email.isBlank()
        ) {
            return false;
        }

        return Objects.equals(
            normalizeEmail(
                email
            ),
            normalizeEmail(
                candidateEmail
            )
        );
    }

    // =========================================================
    // DOMAIN HELPERS · AUTHENTICATION
    // =========================================================

    public boolean canAuthenticate() {

        return active
            && email != null
            && !email.isBlank()
            && passwordHash != null
            && !passwordHash.isBlank();
    }

    public void registerSuccessfulLogin() {

        lastLoginAt =
            OffsetDateTime.now();
    }

    // =========================================================
    // DOMAIN HELPERS · STATUS
    // =========================================================

    public void activate() {

        active =
            true;
    }

    public void deactivate() {

        active =
            false;
    }

    public void changeActiveStatus(
        boolean active
    ) {

        this.active =
            active;
    }

    public void markEmailAsVerified() {

        emailVerified =
            true;
    }

    public void markEmailAsUnverified() {

        emailVerified =
            false;
    }

    // =========================================================
    // DOMAIN HELPERS · PROFILE MUTATION
    // =========================================================

    public void changeName(
        String firstName,
        String lastName
    ) {

        if (
            firstName == null
                || firstName.isBlank()
        ) {
            throw new IllegalArgumentException(
                "firstName es obligatorio."
            );
        }

        if (
            lastName == null
                || lastName.isBlank()
        ) {
            throw new IllegalArgumentException(
                "lastName es obligatorio."
            );
        }

        this.firstName =
            firstName.trim();

        this.lastName =
            lastName.trim();

        validateFirstName();

        validateLastName();
    }

    public void changeEmail(
        String email
    ) {

        if (
            email == null
                || email.isBlank()
        ) {
            throw new IllegalArgumentException(
                "email es obligatorio."
            );
        }

        this.email =
            normalizeEmail(
                email
            );

        validateEmail();
    }

    public void changePasswordHash(
        String passwordHash
    ) {

        if (
            passwordHash == null
                || passwordHash.isBlank()
        ) {
            throw new IllegalArgumentException(
                "passwordHash es obligatorio."
            );
        }

        this.passwordHash =
            passwordHash;

        validatePasswordHash();
    }

    // =========================================================
    // INTERNAL HELPERS
    // =========================================================

    private String normalizeEmail(
        String value
    ) {

        return value
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }
}