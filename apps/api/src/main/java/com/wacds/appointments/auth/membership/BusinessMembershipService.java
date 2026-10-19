package com.wacds.appointments.auth.membership;

import com.wacds.appointments.auth.security.AuthenticatedUser;
import com.wacds.appointments.error.ConflictException;
import com.wacds.appointments.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BusinessMembershipService {

    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final BusinessMembershipRepository
        businessMembershipRepository;

    // =========================================================
    // CURRENT BUSINESS ACCESS
    // =========================================================

    /**
     * Comprueba que el usuario autenticado tiene una membership
     * activa y válida dentro del business indicado.
     *
     * Esta es la barrera principal multi-tenant.
     */
    @Transactional(readOnly = true)
    public BusinessMembership requireBusinessAccess(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        validateAuthenticatedUser(
            authenticatedUser
        );

        validateBusinessId(
            businessId
        );

        if (
            !Objects.equals(
                authenticatedUser.getBusinessId(),
                businessId
            )
        ) {
            throw new AccessDeniedException(
                "No tienes acceso al negocio solicitado."
            );
        }

        BusinessMembership membership =
            businessMembershipRepository
                .findByBusinessIdAndUserIdAndActiveTrue(
                    businessId,
                    authenticatedUser.getUserId()
                )
                .orElseThrow(() ->
                    new AccessDeniedException(
                        "No existe una membresía activa "
                            + "para el negocio solicitado."
                    )
                );

        validateMembership(
            authenticatedUser,
            membership,
            businessId
        );

        return membership;
    }

    // =========================================================
    // OPERATIONAL ACCESS
    // OWNER / ADMIN / STAFF
    // =========================================================

    @Transactional(readOnly = true)
    public BusinessMembership requireOperationalAccess(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        return requireAnyRole(
            authenticatedUser,
            businessId,
            BusinessRole.OWNER,
            BusinessRole.ADMIN,
            BusinessRole.STAFF
        );
    }

    // =========================================================
    // BUSINESS MANAGEMENT
    // OWNER / ADMIN
    // =========================================================

    @Transactional(readOnly = true)
    public BusinessMembership requireBusinessManagement(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        return requireAnyRole(
            authenticatedUser,
            businessId,
            BusinessRole.OWNER,
            BusinessRole.ADMIN
        );
    }

    // =========================================================
    // OWNER ONLY
    // =========================================================

    @Transactional(readOnly = true)
    public BusinessMembership requireOwner(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        return requireAnyRole(
            authenticatedUser,
            businessId,
            BusinessRole.OWNER
        );
    }

    // =========================================================
    // TEAM MANAGEMENT
    // OWNER / ADMIN
    // =========================================================

    /**
     * OWNER:
     *
     * - gestiona ADMIN
     * - gestiona STAFF
     *
     * ADMIN:
     *
     * - gestiona STAFF
     *
     * STAFF:
     *
     * - sin acceso
     */
    @Transactional(readOnly = true)
    public BusinessMembership requireTeamManagement(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        return requireBusinessManagement(
            authenticatedUser,
            businessId
        );
    }

    // =========================================================
    // SENSITIVE MANAGEMENT
    // OWNER ONLY
    // =========================================================

    @Transactional(readOnly = true)
    public BusinessMembership requireSensitiveBusinessManagement(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        return requireOwner(
            authenticatedUser,
            businessId
        );
    }

    // =========================================================
    // BILLING
    // OWNER ONLY
    // =========================================================

    @Transactional(readOnly = true)
    public BusinessMembership requireBillingAccess(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        return requireOwner(
            authenticatedUser,
            businessId
        );
    }

    // =========================================================
    // ROLE AUTHORIZATION
    // =========================================================

    @Transactional(readOnly = true)
    public BusinessMembership requireAnyRole(
        AuthenticatedUser authenticatedUser,
        Long businessId,
        BusinessRole... allowedRoles
    ) {

        BusinessMembership membership =
            requireBusinessAccess(
                authenticatedUser,
                businessId
            );

        Set<BusinessRole> roles =
            normalizeAllowedRoles(
                allowedRoles
            );

        if (
            !roles.contains(
                membership.getRole()
            )
        ) {
            throw new AccessDeniedException(
                "No tienes permisos suficientes "
                    + "para realizar esta operación."
            );
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public BusinessMembership requireRole(
        AuthenticatedUser authenticatedUser,
        Long businessId,
        BusinessRole requiredRole
    ) {

        if (
            requiredRole == null
        ) {
            throw new IllegalArgumentException(
                "requiredRole es obligatorio."
            );
        }

        return requireAnyRole(
            authenticatedUser,
            businessId,
            requiredRole
        );
    }

    // =========================================================
    // TEAM · LIST ALL MEMBERS
    // =========================================================

    /**
     * Lista administrativa completa.
     *
     * Incluye:
     *
     * - memberships activas
     * - memberships inactivas
     *
     * Esto es lo que debe utilizar el panel de Equipo.
     *
     * OWNER / ADMIN:
     *
     * - acceso permitido
     *
     * STAFF:
     *
     * - 403
     */
    @Transactional(readOnly = true)
    public List<BusinessMembership>
        findMembershipsForBusiness(
            AuthenticatedUser authenticatedUser,
            Long businessId
        ) {

        requireTeamManagement(
            authenticatedUser,
            businessId
        );

        return findMembershipsForBusiness(
            businessId
        );
    }

    // =========================================================
    // TEAM · LIST ACTIVE MEMBERS
    // =========================================================

    /**
     * Se conserva para aquellos flujos internos donde
     * únicamente interesen miembros activos.
     *
     * El panel administrativo NO debe utilizar este método.
     */
    @Transactional(readOnly = true)
    public List<BusinessMembership>
        findActiveMembershipsForBusiness(
            AuthenticatedUser authenticatedUser,
            Long businessId
        ) {

        requireTeamManagement(
            authenticatedUser,
            businessId
        );

        return findActiveMembershipsForBusiness(
            businessId
        );
    }

    // =========================================================
    // TEAM · FIND MEMBER
    // =========================================================

    @Transactional(readOnly = true)
    public BusinessMembership findMembershipForManagement(
        AuthenticatedUser authenticatedUser,
        Long businessId,
        Long membershipId
    ) {

        requireTeamManagement(
            authenticatedUser,
            businessId
        );

        BusinessMembership target =
            findMembershipEntity(
                membershipId
            );

        validateTargetMembershipBusiness(
            target,
            businessId
        );

        return target;
    }

    // =========================================================
    // TEAM · UPDATE ROLE
    // =========================================================

    /**
     * OWNER:
     *
     * - ADMIN -> ADMIN
     * - ADMIN -> STAFF
     * - STAFF -> STAFF
     * - STAFF -> ADMIN
     *
     * ADMIN:
     *
     * - únicamente puede gestionar STAFF
     * - únicamente puede asignar STAFF
     *
     * No se permite:
     *
     * - gestionar OWNER
     * - asignar OWNER
     * - modificar la propia membership
     */
    @Transactional
    public BusinessMembership updateMemberRole(
        AuthenticatedUser authenticatedUser,
        Long businessId,
        Long membershipId,
        BusinessRole newRole
    ) {

        validateTargetRole(
            newRole
        );

        BusinessMembership actor =
            requireTeamManagement(
                authenticatedUser,
                businessId
            );

        BusinessMembership target =
            findMembershipEntity(
                membershipId
            );

        validateTargetMembershipBusiness(
            target,
            businessId
        );

        validateNotSelfManagement(
            authenticatedUser,
            target
        );

        validateMemberManagementPermission(
            actor,
            target
        );

        validateRoleAssignmentPermission(
            actor,
            newRole
        );

        if (
            target.getRole()
                == newRole
        ) {
            return target;
        }

        target.setRole(
            newRole
        );

        return businessMembershipRepository.save(
            target
        );
    }

    // =========================================================
    // TEAM · UPDATE STATUS
    // =========================================================

    /**
     * Activa o desactiva una membership.
     *
     * IMPORTANTE:
     *
     * No modifica User.active.
     *
     * Solo modifica el acceso del usuario a este business.
     */
    @Transactional
    public BusinessMembership updateMemberActiveStatus(
        AuthenticatedUser authenticatedUser,
        Long businessId,
        Long membershipId,
        boolean active
    ) {

        BusinessMembership actor =
            requireTeamManagement(
                authenticatedUser,
                businessId
            );

        BusinessMembership target =
            findMembershipEntity(
                membershipId
            );

        validateTargetMembershipBusiness(
            target,
            businessId
        );

        validateNotSelfManagement(
            authenticatedUser,
            target
        );

        validateMemberManagementPermission(
            actor,
            target
        );

        if (
            target.isActive()
                == active
        ) {
            return target;
        }

        target.setActive(
            active
        );

        return businessMembershipRepository.save(
            target
        );
    }

    // =========================================================
    // TEAM · CAN MANAGE MEMBER
    // =========================================================

    @Transactional(readOnly = true)
    public boolean canManageMember(
        AuthenticatedUser authenticatedUser,
        Long businessId,
        Long membershipId
    ) {

        try {

            BusinessMembership actor =
                requireTeamManagement(
                    authenticatedUser,
                    businessId
                );

            BusinessMembership target =
                findMembershipEntity(
                    membershipId
                );

            validateTargetMembershipBusiness(
                target,
                businessId
            );

            validateNotSelfManagement(
                authenticatedUser,
                target
            );

            validateMemberManagementPermission(
                actor,
                target
            );

            return true;

        } catch (
            AccessDeniedException
                | IllegalArgumentException
                | ResourceNotFoundException
                | ConflictException exception
        ) {

            return false;
        }
    }

    // =========================================================
    // BOOLEAN · BUSINESS ACCESS
    // =========================================================

    @Transactional(readOnly = true)
    public boolean canAccessBusiness(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        if (
            authenticatedUser == null
                || authenticatedUser.getUserId() == null
                || authenticatedUser.getBusinessId() == null
                || authenticatedUser.getRole() == null
                || businessId == null
        ) {
            return false;
        }

        if (
            !Objects.equals(
                authenticatedUser.getBusinessId(),
                businessId
            )
        ) {
            return false;
        }

        return businessMembershipRepository
            .findByBusinessIdAndUserIdAndActiveTrue(
                businessId,
                authenticatedUser.getUserId()
            )
            .filter(
                this::isUsableMembership
            )
            .filter(membership ->
                membership.getRole()
                    == authenticatedUser.getRole()
            )
            .isPresent();
    }

    // =========================================================
    // BOOLEAN · OPERATIONAL
    // =========================================================

    @Transactional(readOnly = true)
    public boolean canOperateBusiness(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        try {

            requireOperationalAccess(
                authenticatedUser,
                businessId
            );

            return true;

        } catch (
            AccessDeniedException
                | IllegalArgumentException exception
        ) {

            return false;
        }
    }

    // =========================================================
    // BOOLEAN · MANAGEMENT
    // =========================================================

    @Transactional(readOnly = true)
    public boolean canManageBusiness(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        try {

            requireBusinessManagement(
                authenticatedUser,
                businessId
            );

            return true;

        } catch (
            AccessDeniedException
                | IllegalArgumentException exception
        ) {

            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canManageTeam(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        try {

            requireTeamManagement(
                authenticatedUser,
                businessId
            );

            return true;

        } catch (
            AccessDeniedException
                | IllegalArgumentException exception
        ) {

            return false;
        }
    }

    // =========================================================
    // BOOLEAN · OWNER
    // =========================================================

    @Transactional(readOnly = true)
    public boolean isOwner(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        try {

            requireOwner(
                authenticatedUser,
                businessId
            );

            return true;

        } catch (
            AccessDeniedException
                | IllegalArgumentException exception
        ) {

            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canManageSensitiveBusinessOperations(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        return isOwner(
            authenticatedUser,
            businessId
        );
    }

    @Transactional(readOnly = true)
    public boolean canAccessBilling(
        AuthenticatedUser authenticatedUser,
        Long businessId
    ) {

        return isOwner(
            authenticatedUser,
            businessId
        );
    }

    // =========================================================
    // MEMBERSHIP QUERIES · USER
    // =========================================================

    /**
     * Devuelve todas las memberships activas de un User.
     *
     * Permitirá posteriormente implementar selección
     * de workspace/business.
     */
    @Transactional(readOnly = true)
    public List<BusinessMembership>
        findActiveMembershipsForUser(
            Long userId
        ) {

        validateUserId(
            userId
        );

        return businessMembershipRepository
            .findAllByUserIdAndActiveTrueOrderByCreatedAtAsc(
                userId
            );
    }

    // =========================================================
    // MEMBERSHIP QUERIES · BUSINESS · ALL
    // =========================================================

    /**
     * Consulta interna.
     *
     * Devuelve activas + inactivas.
     *
     * No realiza autorización.
     */
    @Transactional(readOnly = true)
    public List<BusinessMembership>
        findMembershipsForBusiness(
            Long businessId
        ) {

        validateBusinessId(
            businessId
        );

        return businessMembershipRepository
            .findAllByBusinessIdOrderByCreatedAtAsc(
                businessId
            );
    }

    // =========================================================
    // MEMBERSHIP QUERIES · BUSINESS · ACTIVE
    // =========================================================

    /**
     * Consulta interna.
     *
     * Devuelve únicamente memberships activas.
     */
    @Transactional(readOnly = true)
    public List<BusinessMembership>
        findActiveMembershipsForBusiness(
            Long businessId
        ) {

        validateBusinessId(
            businessId
        );

        return businessMembershipRepository
            .findAllByBusinessIdAndActiveTrueOrderByCreatedAtAsc(
                businessId
            );
    }

    // =========================================================
    // MEMBERSHIP QUERIES · BUSINESS + ROLE
    // =========================================================

    @Transactional(readOnly = true)
    public List<BusinessMembership>
        findActiveMembershipsForBusinessByRole(
            Long businessId,
            BusinessRole role
        ) {

        validateBusinessId(
            businessId
        );

        if (
            role == null
        ) {
            throw new IllegalArgumentException(
                "role es obligatorio."
            );
        }

        return businessMembershipRepository
            .findAllByBusinessIdAndRoleAndActiveTrueOrderByCreatedAtAsc(
                businessId,
                role
            );
    }

    // =========================================================
    // MEMBERSHIP ENTITY LOOKUP
    // =========================================================

    /**
     * BusinessMembershipRepository.findById(...) carga también
     * business y user mediante EntityGraph.
     */
    private BusinessMembership findMembershipEntity(
        Long membershipId
    ) {

        validateMembershipId(
            membershipId
        );

        return businessMembershipRepository
            .findById(
                membershipId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe ninguna membresía con id: "
                        + membershipId
                )
            );
    }

    // =========================================================
    // TARGET · BUSINESS VALIDATION
    // =========================================================

    private void validateTargetMembershipBusiness(
        BusinessMembership membership,
        Long expectedBusinessId
    ) {

        validateBusinessId(
            expectedBusinessId
        );

        if (
            membership == null
        ) {
            throw new ResourceNotFoundException(
                "La membresía solicitada no existe."
            );
        }

        if (
            membership.getBusiness() == null
                || membership
                    .getBusiness()
                    .getId() == null
        ) {
            throw new ConflictException(
                "La membresía no tiene un negocio válido asociado."
            );
        }

        if (
            !Objects.equals(
                membership
                    .getBusiness()
                    .getId(),
                expectedBusinessId
            )
        ) {

            /*
             * Deliberadamente no revelamos a qué tenant
             * pertenece la membership.
             */
            throw new AccessDeniedException(
                "No tienes acceso a la membresía solicitada."
            );
        }
    }

    // =========================================================
    // TARGET · SELF MANAGEMENT
    // =========================================================

    /**
     * La propia membership no se modifica mediante
     * las operaciones genéricas de equipo.
     */
    private void validateNotSelfManagement(
        AuthenticatedUser authenticatedUser,
        BusinessMembership target
    ) {

        validateAuthenticatedUser(
            authenticatedUser
        );

        if (
            target == null
                || target.getUser() == null
                || target
                    .getUser()
                    .getId() == null
        ) {
            throw new ConflictException(
                "La membresía objetivo no tiene "
                    + "un usuario válido asociado."
            );
        }

        if (
            Objects.equals(
                authenticatedUser.getUserId(),
                target
                    .getUser()
                    .getId()
            )
        ) {
            throw new ConflictException(
                "No puedes modificar tu propia membresía "
                    + "desde la gestión de equipo."
            );
        }
    }

    // =========================================================
    // TARGET · MANAGEMENT HIERARCHY
    // =========================================================

    private void validateMemberManagementPermission(
        BusinessMembership actor,
        BusinessMembership target
    ) {

        if (
            actor == null
                || actor.getRole() == null
        ) {
            throw new AccessDeniedException(
                "La membresía del usuario actual no es válida."
            );
        }

        if (
            target == null
                || target.getRole() == null
        ) {
            throw new ConflictException(
                "La membresía objetivo no tiene un rol válido."
            );
        }

        BusinessRole actorRole =
            actor.getRole();

        BusinessRole targetRole =
            target.getRole();

        // -----------------------------------------------------
        // OWNER TARGET IS ALWAYS PROTECTED
        // -----------------------------------------------------

        if (
            targetRole
                == BusinessRole.OWNER
        ) {
            throw new AccessDeniedException(
                "El propietario del negocio no puede modificarse "
                    + "desde la gestión general de equipo."
            );
        }

        // -----------------------------------------------------
        // OWNER ACTOR
        // -----------------------------------------------------

        if (
            actorRole
                == BusinessRole.OWNER
        ) {
            return;
        }

        // -----------------------------------------------------
        // ADMIN ACTOR
        // -----------------------------------------------------

        if (
            actorRole
                == BusinessRole.ADMIN
        ) {

            if (
                targetRole
                    == BusinessRole.STAFF
            ) {
                return;
            }

            throw new AccessDeniedException(
                "Un administrador únicamente puede gestionar "
                    + "miembros STAFF."
            );
        }

        // -----------------------------------------------------
        // STAFF / UNKNOWN
        // -----------------------------------------------------

        throw new AccessDeniedException(
            "No tienes permisos para gestionar miembros del equipo."
        );
    }

    // =========================================================
    // ROLE ASSIGNMENT
    // =========================================================

    private void validateRoleAssignmentPermission(
        BusinessMembership actor,
        BusinessRole newRole
    ) {

        if (
            actor == null
                || actor.getRole() == null
        ) {
            throw new AccessDeniedException(
                "La membresía del usuario actual no es válida."
            );
        }

        validateTargetRole(
            newRole
        );

        // -----------------------------------------------------
        // OWNER CANNOT BE ASSIGNED HERE
        // -----------------------------------------------------

        if (
            newRole
                == BusinessRole.OWNER
        ) {
            throw new AccessDeniedException(
                "El rol OWNER no puede asignarse "
                    + "desde la gestión general de equipo."
            );
        }

        // -----------------------------------------------------
        // OWNER ACTOR
        // -----------------------------------------------------

        if (
            actor.getRole()
                == BusinessRole.OWNER
        ) {

            if (
                newRole == BusinessRole.ADMIN
                    || newRole == BusinessRole.STAFF
            ) {
                return;
            }

            throw new AccessDeniedException(
                "El propietario no puede asignar "
                    + "el rol solicitado."
            );
        }

        // -----------------------------------------------------
        // ADMIN ACTOR
        // -----------------------------------------------------

        if (
            actor.getRole()
                == BusinessRole.ADMIN
        ) {

            if (
                newRole
                    == BusinessRole.STAFF
            ) {
                return;
            }

            throw new AccessDeniedException(
                "Un administrador únicamente puede asignar "
                    + "el rol STAFF."
            );
        }

        throw new AccessDeniedException(
            "No tienes permisos para cambiar roles."
        );
    }

    // =========================================================
    // AUTHENTICATED USER VALIDATION
    // =========================================================

    private void validateAuthenticatedUser(
        AuthenticatedUser authenticatedUser
    ) {

        if (
            authenticatedUser == null
        ) {
            throw new AccessDeniedException(
                "No existe una sesión autenticada válida."
            );
        }

        if (
            authenticatedUser.getUserId() == null
        ) {
            throw new AccessDeniedException(
                "La sesión autenticada no contiene "
                    + "un userId válido."
            );
        }

        if (
            authenticatedUser.getBusinessId() == null
        ) {
            throw new AccessDeniedException(
                "La sesión autenticada no contiene "
                    + "un businessId válido."
            );
        }

        if (
            authenticatedUser.getRole() == null
        ) {
            throw new AccessDeniedException(
                "La sesión autenticada no contiene "
                    + "un rol válido."
            );
        }
    }

    // =========================================================
    // ID VALIDATION
    // =========================================================

    private void validateUserId(
        Long userId
    ) {

        if (
            userId == null
        ) {
            throw new IllegalArgumentException(
                "userId es obligatorio."
            );
        }

        if (
            userId <= 0
        ) {
            throw new IllegalArgumentException(
                "userId debe ser mayor que 0."
            );
        }
    }

    private void validateBusinessId(
        Long businessId
    ) {

        if (
            businessId == null
        ) {
            throw new IllegalArgumentException(
                "businessId es obligatorio."
            );
        }

        if (
            businessId <= 0
        ) {
            throw new IllegalArgumentException(
                "businessId debe ser mayor que 0."
            );
        }
    }

    private void validateMembershipId(
        Long membershipId
    ) {

        if (
            membershipId == null
        ) {
            throw new IllegalArgumentException(
                "membershipId es obligatorio."
            );
        }

        if (
            membershipId <= 0
        ) {
            throw new IllegalArgumentException(
                "membershipId debe ser mayor que 0."
            );
        }
    }

    // =========================================================
    // ROLE VALIDATION
    // =========================================================

    private void validateTargetRole(
        BusinessRole role
    ) {

        if (
            role == null
        ) {
            throw new IllegalArgumentException(
                "role es obligatorio."
            );
        }
    }

    // =========================================================
    // CURRENT MEMBERSHIP VALIDATION
    // =========================================================

    /**
     * La base de datos manda sobre el JWT.
     *
     * Esto invalida funcionalmente un token antiguo cuando:
     *
     * - membership se desactiva
     * - User se desactiva
     * - Business se desactiva
     * - cambia el rol
     */
    private void validateMembership(
        AuthenticatedUser authenticatedUser,
        BusinessMembership membership,
        Long expectedBusinessId
    ) {

        validateAuthenticatedUser(
            authenticatedUser
        );

        validateBusinessId(
            expectedBusinessId
        );

        if (
            membership == null
        ) {
            throw new AccessDeniedException(
                "La membresía solicitada no existe."
            );
        }

        if (
            !membership.isActive()
        ) {
            throw new AccessDeniedException(
                "La membresía está desactivada."
            );
        }

        // -----------------------------------------------------
        // USER
        // -----------------------------------------------------

        if (
            membership.getUser() == null
                || membership
                    .getUser()
                    .getId() == null
        ) {
            throw new AccessDeniedException(
                "La membresía no tiene un usuario válido."
            );
        }

        if (
            !Objects.equals(
                membership
                    .getUser()
                    .getId(),
                authenticatedUser.getUserId()
            )
        ) {
            throw new AccessDeniedException(
                "La membresía no pertenece al usuario autenticado."
            );
        }

        if (
            !membership
                .getUser()
                .isActive()
        ) {
            throw new AccessDeniedException(
                "El usuario asociado a la membresía "
                    + "está desactivado."
            );
        }

        // -----------------------------------------------------
        // BUSINESS
        // -----------------------------------------------------

        if (
            membership.getBusiness() == null
                || membership
                    .getBusiness()
                    .getId() == null
        ) {
            throw new AccessDeniedException(
                "La membresía no tiene un negocio válido."
            );
        }

        Long membershipBusinessId =
            membership
                .getBusiness()
                .getId();

        if (
            !Objects.equals(
                membershipBusinessId,
                expectedBusinessId
            )
        ) {
            throw new AccessDeniedException(
                "La membresía no pertenece al negocio solicitado."
            );
        }

        if (
            !Objects.equals(
                membershipBusinessId,
                authenticatedUser.getBusinessId()
            )
        ) {
            throw new AccessDeniedException(
                "La membresía no pertenece al negocio "
                    + "de la sesión actual."
            );
        }

        if (
            !membership
                .getBusiness()
                .isActive()
        ) {
            throw new AccessDeniedException(
                "El negocio asociado a la membresía "
                    + "está desactivado."
            );
        }

        // -----------------------------------------------------
        // ROLE
        // -----------------------------------------------------

        if (
            membership.getRole() == null
        ) {
            throw new AccessDeniedException(
                "La membresía no tiene un rol válido."
            );
        }

        if (
            membership.getRole()
                != authenticatedUser.getRole()
        ) {
            throw new AccessDeniedException(
                "El rol de la sesión ya no coincide "
                    + "con el rol actual del usuario."
            );
        }
    }

    // =========================================================
    // MEMBERSHIP BOOLEAN VALIDATION
    // =========================================================

    private boolean isUsableMembership(
        BusinessMembership membership
    ) {

        if (
            membership == null
        ) {
            return false;
        }

        if (
            !membership.isActive()
        ) {
            return false;
        }

        if (
            membership.getUser() == null
                || membership
                    .getUser()
                    .getId() == null
        ) {
            return false;
        }

        if (
            !membership
                .getUser()
                .isActive()
        ) {
            return false;
        }

        if (
            membership.getBusiness() == null
                || membership
                    .getBusiness()
                    .getId() == null
        ) {
            return false;
        }

        if (
            !membership
                .getBusiness()
                .isActive()
        ) {
            return false;
        }

        return membership.getRole() != null;
    }

    // =========================================================
    // ROLE NORMALIZATION
    // =========================================================

    private Set<BusinessRole> normalizeAllowedRoles(
        BusinessRole... allowedRoles
    ) {

        if (
            allowedRoles == null
                || allowedRoles.length == 0
        ) {
            throw new IllegalArgumentException(
                "Debe indicarse al menos un rol permitido."
            );
        }

        boolean containsNull =
            Arrays.stream(
                    allowedRoles
                )
                .anyMatch(
                    Objects::isNull
                );

        if (
            containsNull
        ) {
            throw new IllegalArgumentException(
                "Los roles permitidos no pueden contener null."
            );
        }

        return EnumSet.copyOf(
            Arrays.asList(
                allowedRoles
            )
        );
    }
}