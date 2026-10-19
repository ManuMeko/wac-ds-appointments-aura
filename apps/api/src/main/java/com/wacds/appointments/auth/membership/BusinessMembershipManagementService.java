package com.wacds.appointments.auth.membership;

import com.wacds.appointments.auth.membership.dto.BusinessMembershipCreateRequest;
import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.auth.user.UserRepository;
import com.wacds.appointments.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BusinessMembershipManagementService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessMembershipRepository
        businessMembershipRepository;

    private final BusinessMembershipService
        businessMembershipService;

    private final UserRepository
        userRepository;

    private final PasswordEncoder
        passwordEncoder;

    // =========================================================
    // CREATE / ADD MEMBER
    // =========================================================

    /**
     * Añade un miembro al business actual.
     *
     * Puede resolver tres escenarios:
     *
     * 1. USER NUEVO
     *
     *    - crea User
     *    - codifica temporaryPassword con BCrypt
     *    - crea BusinessMembership
     *
     * 2. USER EXISTENTE SIN MEMBERSHIP
     *
     *    - reutiliza User
     *    - no modifica password
     *    - no modifica perfil
     *    - crea BusinessMembership
     *
     * 3. USER EXISTENTE CON MEMBERSHIP INACTIVA
     *
     *    - reutiliza BusinessMembership
     *    - valida jerarquía de roles
     *    - actualiza rol
     *    - reactiva membership
     *
     * Si la membership ya está activa:
     *
     *    - 409 Conflict
     *
     * Reglas:
     *
     * OWNER:
     *
     *    - puede añadir ADMIN
     *    - puede añadir STAFF
     *
     * ADMIN:
     *
     *    - puede añadir STAFF
     *    - no puede añadir ADMIN
     *
     * STAFF:
     *
     *    - no puede gestionar equipo
     *
     * OWNER nunca puede asignarse desde este flujo.
     */
    @Transactional
    public BusinessMembership createMember(
        AuthenticatedUser authenticatedUser,
        Long businessId,
        BusinessMembershipCreateRequest request
    ) {

        validateRequest(
            request
        );

        BusinessMembership actorMembership =
            businessMembershipService
                .requireTeamManagement(
                    authenticatedUser,
                    businessId
                );

        BusinessRole requestedRole =
            request.role();

        validateRequestedRole(
            actorMembership,
            requestedRole
        );

        String normalizedEmail =
            normalizeEmail(
                request.email()
            );

        User user =
            userRepository
                .findByEmailIgnoreCase(
                    normalizedEmail
                )
                .orElse(null);

        // =====================================================
        // EXISTING USER
        // =====================================================

        if (user != null) {

            return addExistingUserToBusiness(
                authenticatedUser,
                businessId,
                actorMembership,
                user,
                requestedRole
            );
        }

        // =====================================================
        // NEW USER
        // =====================================================

        return createNewUserAndMembership(
            actorMembership,
            normalizedEmail,
            request,
            requestedRole
        );
    }

    // =========================================================
    // EXISTING USER
    // =========================================================

    private BusinessMembership addExistingUserToBusiness(
        AuthenticatedUser authenticatedUser,
        Long businessId,
        BusinessMembership actorMembership,
        User user,
        BusinessRole requestedRole
    ) {

        if (
            user.getId() == null
        ) {
            throw new IllegalStateException(
                "El usuario existente no está correctamente persistido."
            );
        }

        /*
         * Una cuenta global desactivada no debe reactivarse
         * implícitamente desde gestión de miembros.
         *
         * Esa operación debe pertenecer a un flujo administrativo
         * explícito.
         */
        if (
            !user.isActive()
        ) {
            throw new ConflictException(
                "Existe un usuario con ese email, "
                    + "pero su cuenta global está desactivada."
            );
        }

        BusinessMembership existingMembership =
            businessMembershipRepository
                .findByBusinessIdAndUserId(
                    businessId,
                    user.getId()
                )
                .orElse(null);

        // -----------------------------------------------------
        // NO EXISTING MEMBERSHIP
        // -----------------------------------------------------

        if (
            existingMembership == null
        ) {

            return createMembership(
                actorMembership,
                user,
                requestedRole
            );
        }

        // -----------------------------------------------------
        // ACTIVE MEMBERSHIP ALREADY EXISTS
        // -----------------------------------------------------

        if (
            existingMembership.isActive()
        ) {
            throw new ConflictException(
                "El usuario ya pertenece activamente "
                    + "a este negocio."
            );
        }

        // -----------------------------------------------------
        // INACTIVE MEMBERSHIP · REACTIVATE
        // -----------------------------------------------------

        /*
         * Utilizamos BusinessMembershipService para reaplicar
         * todas las reglas de jerarquía sobre el target existente.
         *
         * Así evitamos duplicar reglas como:
         *
         * OWNER -> ADMIN / STAFF
         * ADMIN -> solo STAFF
         * OWNER target -> protegido
         * self-management -> protegido
         */
        BusinessMembership roleUpdated =
            businessMembershipService
                .updateMemberRole(
                    authenticatedUser,
                    businessId,
                    existingMembership.getId(),
                    requestedRole
                );

        /*
         * Después reactivamos la membership usando exactamente
         * la misma capa central de autorización.
         */
        return businessMembershipService
            .updateMemberActiveStatus(
                authenticatedUser,
                businessId,
                roleUpdated.getId(),
                true
            );
    }

    // =========================================================
    // NEW USER
    // =========================================================

    private BusinessMembership createNewUserAndMembership(
        BusinessMembership actorMembership,
        String normalizedEmail,
        BusinessMembershipCreateRequest request,
        BusinessRole requestedRole
    ) {

        validateNewUserRequest(
            request
        );

        /*
         * Defensa adicional frente a posibles inconsistencias
         * entre la búsqueda previa y el momento de creación.
         */
        if (
            userRepository.existsByEmailIgnoreCase(
                normalizedEmail
            )
        ) {
            throw new ConflictException(
                "Ya existe un usuario con ese email."
            );
        }

        String encodedPassword =
            passwordEncoder.encode(
                request.temporaryPassword()
            );

        User user =
            User
                .builder()
                .email(
                    normalizedEmail
                )
                .passwordHash(
                    encodedPassword
                )
                .firstName(
                    request
                        .firstName()
                        .trim()
                )
                .lastName(
                    request
                        .lastName()
                        .trim()
                )
                .active(
                    true
                )
                .emailVerified(
                    false
                )
                .build();

        User savedUser =
            userRepository.save(
                user
            );

        return createMembership(
            actorMembership,
            savedUser,
            requestedRole
        );
    }

    // =========================================================
    // CREATE MEMBERSHIP ENTITY
    // =========================================================

    private BusinessMembership createMembership(
        BusinessMembership actorMembership,
        User user,
        BusinessRole requestedRole
    ) {

        if (
            actorMembership == null
                || actorMembership.getBusiness() == null
                || actorMembership
                    .getBusiness()
                    .getId() == null
        ) {
            throw new IllegalStateException(
                "La membership del usuario actual "
                    + "no tiene un negocio válido."
            );
        }

        if (
            user == null
                || user.getId() == null
        ) {
            throw new IllegalStateException(
                "No se puede crear una membership "
                    + "para un usuario no persistido."
            );
        }

        if (
            requestedRole == null
        ) {
            throw new IllegalArgumentException(
                "requestedRole es obligatorio."
            );
        }

        Long businessId =
            actorMembership
                .getBusiness()
                .getId();

        /*
         * Segunda barrera contra duplicados.
         *
         * La restricción UNIQUE de PostgreSQL seguirá siendo
         * la última defensa ante concurrencia real.
         */
        if (
            businessMembershipRepository
                .existsByBusinessIdAndUserId(
                    businessId,
                    user.getId()
                )
        ) {
            throw new ConflictException(
                "El usuario ya tiene una membership "
                    + "asociada a este negocio."
            );
        }

        BusinessMembership membership =
            BusinessMembership
                .builder()
                .business(
                    actorMembership.getBusiness()
                )
                .user(
                    user
                )
                .role(
                    requestedRole
                )
                .active(
                    true
                )
                .build();

        return businessMembershipRepository.save(
            membership
        );
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
        BusinessMembershipCreateRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                "La solicitud de creación de miembro es obligatoria."
            );
        }

        if (
            request.email() == null
                || request.email().isBlank()
        ) {
            throw new IllegalArgumentException(
                "El email es obligatorio."
            );
        }

        if (
            request.role() == null
        ) {
            throw new IllegalArgumentException(
                "El rol es obligatorio."
            );
        }
    }

    // =========================================================
    // NEW USER REQUEST VALIDATION
    // =========================================================

    private void validateNewUserRequest(
        BusinessMembershipCreateRequest request
    ) {

        if (
            request.firstName() == null
                || request.firstName().isBlank()
        ) {
            throw new IllegalArgumentException(
                "El nombre es obligatorio "
                    + "para crear un usuario nuevo."
            );
        }

        if (
            request.lastName() == null
                || request.lastName().isBlank()
        ) {
            throw new IllegalArgumentException(
                "Los apellidos son obligatorios "
                    + "para crear un usuario nuevo."
            );
        }

        if (
            request.temporaryPassword() == null
                || request.temporaryPassword().isBlank()
        ) {
            throw new IllegalArgumentException(
                "La contraseña temporal es obligatoria "
                    + "para crear un usuario nuevo."
            );
        }

        if (
            request.firstName()
                .trim()
                .length()
                > 100
        ) {
            throw new IllegalArgumentException(
                "El nombre no puede superar los 100 caracteres."
            );
        }

        if (
            request.lastName()
                .trim()
                .length()
                > 150
        ) {
            throw new IllegalArgumentException(
                "Los apellidos no pueden superar los 150 caracteres."
            );
        }

        int passwordLength =
            request
                .temporaryPassword()
                .length();

        if (
            passwordLength < 12
                || passwordLength > 72
        ) {
            throw new IllegalArgumentException(
                "La contraseña temporal debe contener "
                    + "entre 12 y 72 caracteres."
            );
        }
    }

    // =========================================================
    // ROLE AUTHORIZATION
    // =========================================================

    private void validateRequestedRole(
        BusinessMembership actorMembership,
        BusinessRole requestedRole
    ) {

        if (
            actorMembership == null
                || actorMembership.getRole() == null
        ) {
            throw new AccessDeniedException(
                "La membership del usuario actual "
                    + "no tiene un rol válido."
            );
        }

        if (
            requestedRole == null
        ) {
            throw new IllegalArgumentException(
                "El rol solicitado es obligatorio."
            );
        }

        // -----------------------------------------------------
        // OWNER CAN NEVER BE ASSIGNED HERE
        // -----------------------------------------------------

        if (
            requestedRole
                == BusinessRole.OWNER
        ) {
            throw new AccessDeniedException(
                "El rol OWNER no puede asignarse "
                    + "desde la gestión general de equipo."
            );
        }

        BusinessRole actorRole =
            actorMembership.getRole();

        // -----------------------------------------------------
        // OWNER
        // -----------------------------------------------------

        if (
            actorRole
                == BusinessRole.OWNER
        ) {

            if (
                requestedRole
                    == BusinessRole.ADMIN
                    || requestedRole
                        == BusinessRole.STAFF
            ) {
                return;
            }

            throw new AccessDeniedException(
                "El propietario no puede asignar "
                    + "el rol solicitado."
            );
        }

        // -----------------------------------------------------
        // ADMIN
        // -----------------------------------------------------

        if (
            actorRole
                == BusinessRole.ADMIN
        ) {

            if (
                requestedRole
                    == BusinessRole.STAFF
            ) {
                return;
            }

            throw new AccessDeniedException(
                "Un administrador únicamente puede "
                    + "crear miembros STAFF."
            );
        }

        // -----------------------------------------------------
        // STAFF / UNKNOWN
        // -----------------------------------------------------

        throw new AccessDeniedException(
            "No tienes permisos para crear miembros del equipo."
        );
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
                "El email es obligatorio."
            );
        }

        String normalized =
            email
                .trim()
                .toLowerCase(
                    Locale.ROOT
                );

        if (
            normalized.length()
                > 255
        ) {
            throw new IllegalArgumentException(
                "El email no puede superar los 255 caracteres."
            );
        }

        return normalized;
    }
}