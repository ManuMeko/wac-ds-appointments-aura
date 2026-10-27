package com.wacds.appointments.config;

import com.wacds.appointments.auth.membership.BusinessMembership;
import com.wacds.appointments.auth.membership.BusinessMembershipRepository;
import com.wacds.appointments.auth.membership.BusinessRole;
import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.auth.user.UserRepository;
import com.wacds.appointments.business.Business;
import com.wacds.appointments.business.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.bootstrap.dev-owner.enabled",
    havingValue = "true"
)
public class DevelopmentAuthBootstrap
    implements ApplicationRunner {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MINIMUM_PASSWORD_LENGTH =
        12;

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final UserRepository
        userRepository;

    private final BusinessRepository
        businessRepository;

    private final BusinessMembershipRepository
        businessMembershipRepository;

    private final PasswordEncoder
        passwordEncoder;

    // =========================================================
    // BUSINESS CONFIGURATION
    // =========================================================

    @Value(
        "${app.bootstrap.dev-owner.business-slug:aura-studio}"
    )
    private String businessSlug;

    // =========================================================
    // OWNER CONFIGURATION
    // =========================================================

    @Value(
        "${app.bootstrap.dev-owner.email:}"
    )
    private String ownerEmail;

    @Value(
        "${app.bootstrap.dev-owner.password:}"
    )
    private String ownerPassword;

    @Value(
        "${app.bootstrap.dev-owner.first-name:}"
    )
    private String ownerFirstName;

    @Value(
        "${app.bootstrap.dev-owner.last-name:}"
    )
    private String ownerLastName;

    // =========================================================
    // ADMIN CONFIGURATION
    // =========================================================

    /**
     * Los valores por defecto permiten disponer inmediatamente
     * de un usuario ADMIN en el entorno de desarrollo.
     *
     * Si no se configura una contraseña específica para ADMIN,
     * se reutiliza la contraseña configurada para OWNER.
     */
    @Value(
        "${app.bootstrap.dev-owner.admin-email:admin@aurastudio.es}"
    )
    private String adminEmail;

    @Value(
        "${app.bootstrap.dev-owner.admin-password:${app.bootstrap.dev-owner.password:}}"
    )
    private String adminPassword;

    @Value(
        "${app.bootstrap.dev-owner.admin-first-name:Admin}"
    )
    private String adminFirstName;

    @Value(
        "${app.bootstrap.dev-owner.admin-last-name:Aura Studio}"
    )
    private String adminLastName;

    // =========================================================
    // STAFF CONFIGURATION
    // =========================================================

    /**
     * Usuario STAFF utilizado para validar los permisos
     * operativos y las restricciones RBAC.
     *
     * Si no se configura otra contraseña se reutiliza
     * la contraseña del OWNER.
     */
    @Value(
        "${app.bootstrap.dev-owner.staff-email:staff@aurastudio.es}"
    )
    private String staffEmail;

    @Value(
        "${app.bootstrap.dev-owner.staff-password:${app.bootstrap.dev-owner.password:}}"
    )
    private String staffPassword;

    @Value(
        "${app.bootstrap.dev-owner.staff-first-name:Staff}"
    )
    private String staffFirstName;

    @Value(
        "${app.bootstrap.dev-owner.staff-last-name:Aura Studio}"
    )
    private String staffLastName;

    // =========================================================
    // MOTORLINE OWNER CONFIGURATION
    // =========================================================

    /**
     * Owner opcional de Motorline Taller.
     *
     * Email y contraseña llegan solo por el entorno.
     * Si ambos están vacíos, este bloque no hace nada.
     * La contraseña no se reutiliza desde la cuenta de Aura.
     */
    @Value(
        "${app.bootstrap.dev-taller-owner.business-slug:motorline-taller}"
    )
    private String tallerOwnerBusinessSlug;

    @Value(
        "${app.bootstrap.dev-taller-owner.email:}"
    )
    private String tallerOwnerEmail;

    @Value(
        "${app.bootstrap.dev-taller-owner.password:}"
    )
    private String tallerOwnerPassword;

    @Value(
        "${app.bootstrap.dev-taller-owner.first-name:Owner}"
    )
    private String tallerOwnerFirstName;

    @Value(
        "${app.bootstrap.dev-taller-owner.last-name:Motorline}"
    )
    private String tallerOwnerLastName;

    // =========================================================
    // BOOTSTRAP
    // =========================================================

    @Override
    @Transactional
    public void run(
        ApplicationArguments args
    ) {

        log.info(
            "Development authentication bootstrap enabled."
        );

        validateConfiguration();

        String normalizedBusinessSlug =
            normalizeSlug(
                businessSlug
            );

        /*
         * BusinessRepository utiliza búsquedas case-insensitive.
         *
         * Esto mantiene el bootstrap alineado con el repositorio
         * actual y evita depender de la capitalización del slug.
         */
        Business business =
            businessRepository
                .findBySlugIgnoreCase(
                    normalizedBusinessSlug
                )
                .orElseThrow(() ->
                    new IllegalStateException(
                        "No existe ningún negocio con slug '"
                            + normalizedBusinessSlug
                            + "'."
                    )
                );

        if (
            !business.isActive()
        ) {
            throw new IllegalStateException(
                "El negocio '"
                    + normalizedBusinessSlug
                    + "' está desactivado."
            );
        }

        // -----------------------------------------------------
        // OWNER
        // -----------------------------------------------------

        bootstrapAccount(
            business,
            new DevelopmentAccount(
                "OWNER",
                ownerEmail,
                ownerPassword,
                ownerFirstName,
                ownerLastName,
                BusinessRole.OWNER
            )
        );

        // -----------------------------------------------------
        // ADMIN
        // -----------------------------------------------------

        bootstrapAccount(
            business,
            new DevelopmentAccount(
                "ADMIN",
                adminEmail,
                adminPassword,
                adminFirstName,
                adminLastName,
                BusinessRole.ADMIN
            )
        );

        // -----------------------------------------------------
        // STAFF
        // -----------------------------------------------------

        bootstrapAccount(
            business,
            new DevelopmentAccount(
                "STAFF",
                staffEmail,
                staffPassword,
                staffFirstName,
                staffLastName,
                BusinessRole.STAFF
            )
        );

        bootstrapTallerOwner();

        log.info(
            "Development authentication bootstrap completed. "
                + "businessId={}, businessSlug={}",
            business.getId(),
            normalizedBusinessSlug
        );
    }

    // =========================================================
    // MOTORLINE OWNER BOOTSTRAP
    // =========================================================

    /**
     * Crea el OWNER de Motorline solo cuando el entorno
     * trae email y contraseña a la vez.
     *
     * El negocio tiene que existir (seed). Este método
     * no crea el business ni escribe la contraseña en claro.
     */
    private void bootstrapTallerOwner() {

        boolean emailMissing =
            tallerOwnerEmail == null
                || tallerOwnerEmail.isBlank();

        boolean passwordMissing =
            tallerOwnerPassword == null
                || tallerOwnerPassword.isBlank();

        if (
            emailMissing
                && passwordMissing
        ) {

            log.info(
                "Development Motorline owner bootstrap skipped."
            );

            return;
        }

        if (
            emailMissing
                || passwordMissing
        ) {
            throw new IllegalStateException(
                "DEV_TALLER_OWNER_EMAIL y "
                    + "DEV_TALLER_OWNER_PASSWORD "
                    + "deben configurarse juntas."
            );
        }

        DevelopmentAccount account =
            new DevelopmentAccount(
                "OWNER",
                tallerOwnerEmail,
                tallerOwnerPassword,
                tallerOwnerFirstName,
                tallerOwnerLastName,
                BusinessRole.OWNER
            );

        validateDevelopmentAccount(
            account
        );

        String normalizedTallerEmail =
            normalizeEmail(
                tallerOwnerEmail
            );

        rejectIfEmailMatchesAuraAccount(
            normalizedTallerEmail
        );

        String normalizedTallerSlug =
            normalizeSlug(
                tallerOwnerBusinessSlug
            );

        Business tallerBusiness =
            businessRepository
                .findBySlugIgnoreCase(
                    normalizedTallerSlug
                )
                .orElseThrow(() ->
                    new IllegalStateException(
                        "No existe ningún negocio con slug '"
                            + normalizedTallerSlug
                            + "'."
                    )
                );

        if (
            !tallerBusiness.isActive()
        ) {
            throw new IllegalStateException(
                "El negocio '"
                    + normalizedTallerSlug
                    + "' está desactivado."
            );
        }

        rejectIfEmailBelongsToAnotherBusiness(
            normalizedTallerEmail,
            tallerBusiness
        );

        bootstrapAccount(
            tallerBusiness,
            account
        );
    }

    private void rejectIfEmailMatchesAuraAccount(
        String normalizedTallerEmail
    ) {

        boolean sameAsAuraAccount =
            normalizedTallerEmail.equals(
                normalizeEmail(
                    ownerEmail
                )
            )
                || normalizedTallerEmail.equals(
                    normalizeEmail(
                        adminEmail
                    )
                )
                || normalizedTallerEmail.equals(
                    normalizeEmail(
                        staffEmail
                    )
                );

        if (
            sameAsAuraAccount
        ) {
            throw new IllegalStateException(
                "El email del owner de Motorline debe ser "
                    + "distinto de las cuentas de desarrollo de Aura."
            );
        }
    }

    /**
     * El login entra en la membership activa más antigua.
     * Una segunda membership no abriría el panel de Motorline.
     */
    private void rejectIfEmailBelongsToAnotherBusiness(
        String normalizedEmail,
        Business tallerBusiness
    ) {

        User existingUser =
            userRepository
                .findByEmailIgnoreCase(
                    normalizedEmail
                )
                .orElse(
                    null
                );

        if (
            existingUser == null
                || existingUser.getId() == null
        ) {
            return;
        }

        List<BusinessMembership> memberships =
            businessMembershipRepository
                .findAllByUserIdOrderByCreatedAtAsc(
                    existingUser.getId()
                );

        if (
            memberships == null
                || memberships.isEmpty()
        ) {
            return;
        }

        for (
            BusinessMembership membership
                : memberships
        ) {

            if (
                membership == null
                    || membership.getBusiness() == null
                    || membership
                        .getBusiness()
                        .getId() == null
            ) {
                throw new IllegalStateException(
                    "La membership existente del owner "
                        + "de Motorline no es válida."
                );
            }

            boolean sameBusiness =
                membership
                    .getBusiness()
                    .getId()
                    .equals(
                        tallerBusiness.getId()
                    );

            if (
                !sameBusiness
            ) {
                throw new IllegalStateException(
                    "El email del owner de Motorline ya "
                        + "pertenece a otro negocio."
                );
            }
        }
    }

    // =========================================================
    // ACCOUNT BOOTSTRAP
    // =========================================================

    /**
     * Garantiza de forma idempotente que exista:
     *
     * - User
     * - User activo
     * - email verificado
     * - BusinessMembership
     * - Membership activa
     * - rol correcto
     *
     * Puede ejecutarse en cada arranque de desarrollo
     * sin crear duplicados.
     */
    private void bootstrapAccount(
        Business business,
        DevelopmentAccount account
    ) {

        validateBusiness(
            business
        );

        validateDevelopmentAccount(
            account
        );

        String normalizedEmail =
            normalizeEmail(
                account.email()
            );

        User user =
            userRepository
                .findByEmailIgnoreCase(
                    normalizedEmail
                )
                .orElseGet(() ->
                    createUser(
                        account,
                        normalizedEmail
                    )
                );

        ensureUserIsUsable(
            user
        );

        BusinessMembership membership =
            businessMembershipRepository
                .findByBusinessIdAndUserId(
                    business.getId(),
                    user.getId()
                )
                .orElseGet(() ->
                    createMembership(
                        business,
                        user,
                        account.role(),
                        account.label()
                    )
                );

        membership =
            ensureMembershipIsUsable(
                membership,
                account.role()
            );

        log.info(
            "Development {} ready. "
                + "userId={}, email={}, businessId={}, "
                + "businessSlug={}, role={}",
            account.label(),
            user.getId(),
            user.getEmail(),
            business.getId(),
            business.getSlug(),
            membership.getRole()
        );
    }

    // =========================================================
    // USER CREATION
    // =========================================================

    private User createUser(
        DevelopmentAccount account,
        String normalizedEmail
    ) {

        User user =
            User
                .builder()
                .email(
                    normalizedEmail
                )
                .passwordHash(
                    passwordEncoder.encode(
                        account.password()
                    )
                )
                .firstName(
                    account
                        .firstName()
                        .trim()
                )
                .lastName(
                    account
                        .lastName()
                        .trim()
                )
                .active(
                    true
                )
                .emailVerified(
                    true
                )
                .build();

        User savedUser =
            userRepository.save(
                user
            );

        log.info(
            "Development {} user created. "
                + "userId={}, email={}",
            account.label(),
            savedUser.getId(),
            savedUser.getEmail()
        );

        return savedUser;
    }

    // =========================================================
    // MEMBERSHIP CREATION
    // =========================================================

    private BusinessMembership createMembership(
        Business business,
        User user,
        BusinessRole role,
        String label
    ) {

        validateBusiness(
            business
        );

        if (
            user == null
                || user.getId() == null
        ) {
            throw new IllegalStateException(
                "No se puede crear una membership de desarrollo "
                    + "para un usuario no persistido."
            );
        }

        if (
            role == null
        ) {
            throw new IllegalArgumentException(
                "El rol de la membership es obligatorio."
            );
        }

        BusinessMembership membership =
            BusinessMembership
                .builder()
                .business(
                    business
                )
                .user(
                    user
                )
                .role(
                    role
                )
                .active(
                    true
                )
                .build();

        BusinessMembership savedMembership =
            businessMembershipRepository.save(
                membership
            );

        log.info(
            "Development {} membership created. "
                + "membershipId={}, userId={}, "
                + "businessId={}, role={}",
            label,
            savedMembership.getId(),
            user.getId(),
            business.getId(),
            savedMembership.getRole()
        );

        return savedMembership;
    }

    // =========================================================
    // EXISTING USER VALIDATION
    // =========================================================

    /**
     * Si el usuario ya existe pero está desactivado
     * o no tiene el email verificado, el bootstrap
     * lo deja preparado para las pruebas de desarrollo.
     *
     * Esta lógica existe únicamente porque este componente
     * es un bootstrap explícito de entorno development.
     */
    private void ensureUserIsUsable(
        User user
    ) {

        if (
            user == null
                || user.getId() == null
        ) {
            throw new IllegalStateException(
                "El usuario de desarrollo no es válido."
            );
        }

        boolean changed =
            false;

        if (
            !user.isActive()
        ) {

            user.activate();

            changed =
                true;
        }

        if (
            !user.isEmailVerified()
        ) {

            user.markEmailAsVerified();

            changed =
                true;
        }

        if (
            changed
        ) {

            userRepository.save(
                user
            );
        }
    }

    // =========================================================
    // EXISTING MEMBERSHIP VALIDATION
    // =========================================================

    /**
     * Garantiza que la membership existente:
     *
     * - esté activa
     * - mantenga exactamente el rol esperado
     *
     * Esto permite que las cuentas base de desarrollo vuelvan
     * a un estado conocido después de pruebas RBAC.
     *
     * owner@... -> OWNER
     * admin@... -> ADMIN
     * staff@... -> STAFF
     */
    private BusinessMembership ensureMembershipIsUsable(
        BusinessMembership membership,
        BusinessRole expectedRole
    ) {

        if (
            membership == null
                || membership.getId() == null
        ) {
            throw new IllegalStateException(
                "La membresía de desarrollo no es válida."
            );
        }

        if (
            expectedRole == null
        ) {
            throw new IllegalArgumentException(
                "expectedRole es obligatorio."
            );
        }

        boolean changed =
            false;

        if (
            !membership.isActive()
        ) {

            membership.setActive(
                true
            );

            changed =
                true;
        }

        if (
            membership.getRole()
                != expectedRole
        ) {

            membership.setRole(
                expectedRole
            );

            changed =
                true;
        }

        if (
            changed
        ) {

            return businessMembershipRepository.save(
                membership
            );
        }

        return membership;
    }

    // =========================================================
    // BUSINESS VALIDATION
    // =========================================================

    private void validateBusiness(
        Business business
    ) {

        if (
            business == null
        ) {
            throw new IllegalStateException(
                "El business de desarrollo es obligatorio."
            );
        }

        if (
            business.getId() == null
        ) {
            throw new IllegalStateException(
                "El business de desarrollo no está persistido."
            );
        }

        if (
            business.getSlug() == null
                || business.getSlug().isBlank()
        ) {
            throw new IllegalStateException(
                "El business de desarrollo no tiene un slug válido."
            );
        }

        if (
            !business.isActive()
        ) {
            throw new IllegalStateException(
                "El business de desarrollo está desactivado."
            );
        }
    }

    // =========================================================
    // CONFIGURATION VALIDATION
    // =========================================================

    private void validateConfiguration() {

        if (
            businessSlug == null
                || businessSlug.isBlank()
        ) {
            throw new IllegalStateException(
                "app.bootstrap.dev-owner.business-slug "
                    + "es obligatorio."
            );
        }

        validateDevelopmentAccount(
            new DevelopmentAccount(
                "OWNER",
                ownerEmail,
                ownerPassword,
                ownerFirstName,
                ownerLastName,
                BusinessRole.OWNER
            )
        );

        validateDevelopmentAccount(
            new DevelopmentAccount(
                "ADMIN",
                adminEmail,
                adminPassword,
                adminFirstName,
                adminLastName,
                BusinessRole.ADMIN
            )
        );

        validateDevelopmentAccount(
            new DevelopmentAccount(
                "STAFF",
                staffEmail,
                staffPassword,
                staffFirstName,
                staffLastName,
                BusinessRole.STAFF
            )
        );

        String normalizedOwnerEmail =
            normalizeEmail(
                ownerEmail
            );

        String normalizedAdminEmail =
            normalizeEmail(
                adminEmail
            );

        String normalizedStaffEmail =
            normalizeEmail(
                staffEmail
            );

        if (
            normalizedOwnerEmail.equals(
                normalizedAdminEmail
            )
        ) {
            throw new IllegalStateException(
                "OWNER y ADMIN deben utilizar emails diferentes."
            );
        }

        if (
            normalizedOwnerEmail.equals(
                normalizedStaffEmail
            )
        ) {
            throw new IllegalStateException(
                "OWNER y STAFF deben utilizar emails diferentes."
            );
        }

        if (
            normalizedAdminEmail.equals(
                normalizedStaffEmail
            )
        ) {
            throw new IllegalStateException(
                "ADMIN y STAFF deben utilizar emails diferentes."
            );
        }
    }

    // =========================================================
    // DEVELOPMENT ACCOUNT VALIDATION
    // =========================================================

    private void validateDevelopmentAccount(
        DevelopmentAccount account
    ) {

        if (
            account == null
        ) {
            throw new IllegalStateException(
                "La configuración de la cuenta "
                    + "de desarrollo es obligatoria."
            );
        }

        if (
            account.label() == null
                || account.label().isBlank()
        ) {
            throw new IllegalStateException(
                "El identificador de la cuenta "
                    + "de desarrollo es obligatorio."
            );
        }

        if (
            account.email() == null
                || account.email().isBlank()
        ) {
            throw new IllegalStateException(
                "El email del usuario "
                    + account.label()
                    + " de desarrollo es obligatorio."
            );
        }

        if (
            account.password() == null
                || account.password().isBlank()
        ) {
            throw new IllegalStateException(
                "La contraseña del usuario "
                    + account.label()
                    + " de desarrollo es obligatoria."
            );
        }

        if (
            account.password().length()
                < MINIMUM_PASSWORD_LENGTH
        ) {
            throw new IllegalStateException(
                "La contraseña del usuario "
                    + account.label()
                    + " debe tener al menos "
                    + MINIMUM_PASSWORD_LENGTH
                    + " caracteres."
            );
        }

        if (
            account.firstName() == null
                || account.firstName().isBlank()
        ) {
            throw new IllegalStateException(
                "El nombre del usuario "
                    + account.label()
                    + " de desarrollo es obligatorio."
            );
        }

        if (
            account.lastName() == null
                || account.lastName().isBlank()
        ) {
            throw new IllegalStateException(
                "El apellido del usuario "
                    + account.label()
                    + " de desarrollo es obligatorio."
            );
        }

        if (
            account.role() == null
        ) {
            throw new IllegalStateException(
                "El rol del usuario "
                    + account.label()
                    + " de desarrollo es obligatorio."
            );
        }
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
                "El email no puede estar vacío."
            );
        }

        String normalized =
            email
                .trim()
                .toLowerCase(
                    Locale.ROOT
                );

        if (
            normalized.length() > 255
        ) {
            throw new IllegalArgumentException(
                "El email no puede superar los 255 caracteres."
            );
        }

        return normalized;
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
                "El slug no puede estar vacío."
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
    // INTERNAL TYPES
    // =========================================================

    private record DevelopmentAccount(

        String label,

        String email,

        String password,

        String firstName,

        String lastName,

        BusinessRole role

    ) {
    }
}