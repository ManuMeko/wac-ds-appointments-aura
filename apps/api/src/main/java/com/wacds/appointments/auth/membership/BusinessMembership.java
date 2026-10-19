package com.wacds.appointments.auth.membership;

import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.business.Business;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "business_memberships",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "ux_business_membership",
            columnNames = {
                "business_id",
                "user_id"
            }
        )
    },
    indexes = {
        @Index(
            name = "ix_business_memberships_business",
            columnList = "business_id"
        ),
        @Index(
            name = "ix_business_memberships_user",
            columnList = "user_id"
        ),
        @Index(
            name = "ix_business_memberships_role",
            columnList = "role"
        ),
        @Index(
            name = "ix_business_memberships_active",
            columnList = "active"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessMembership {

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

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "business_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_business_memberships_business"
        )
    )
    private Business business;

    // =========================================================
    // USER
    // =========================================================

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_business_memberships_user"
        )
    )
    private User user;

    // =========================================================
    // ROLE
    // =========================================================

    @Enumerated(
        EnumType.STRING
    )
    @Column(
        name = "role",
        nullable = false,
        length = 30
    )
    private BusinessRole role;

    // =========================================================
    // STATUS
    // =========================================================

    @Column(
        name = "active",
        nullable = false
    )
    @Builder.Default
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
    // JPA LIFECYCLE · CREATE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        validateState();

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

        validateState();

        updatedAt =
            OffsetDateTime.now();
    }

    // =========================================================
    // DOMAIN VALIDATION
    // =========================================================

    /**
     * Garantiza que una membership nunca pueda persistirse
     * en un estado estructural inválido.
     *
     * Una membership siempre necesita:
     *
     * - business
     * - user
     * - role
     */
    private void validateState() {

        if (business == null) {
            throw new IllegalStateException(
                "El negocio de la membresía es obligatorio."
            );
        }

        if (user == null) {
            throw new IllegalStateException(
                "El usuario de la membresía es obligatorio."
            );
        }

        if (role == null) {
            throw new IllegalStateException(
                "El rol de la membresía es obligatorio."
            );
        }
    }

    // =========================================================
    // DOMAIN HELPERS · ROLE
    // =========================================================

    public boolean isOwner() {

        return role
            == BusinessRole.OWNER;
    }

    public boolean isAdmin() {

        return role
            == BusinessRole.ADMIN;
    }

    public boolean isStaff() {

        return role
            == BusinessRole.STAFF;
    }

    /**
     * Indica si la membership tiene capacidad
     * de gestión estructural del negocio.
     */
    public boolean canManageBusiness() {

        return isOwner()
            || isAdmin();
    }

    /**
     * OWNER y ADMIN pueden acceder al módulo
     * de gestión de equipo.
     *
     * Las reglas sobre qué miembros concretos puede modificar
     * cada uno se aplican en BusinessMembershipService.
     */
    public boolean canManageTeam() {

        return isOwner()
            || isAdmin();
    }

    /**
     * Indica si puede realizar operaciones operativas
     * del día a día.
     *
     * Actualmente:
     *
     * OWNER
     * ADMIN
     * STAFF
     */
    public boolean canOperateBusiness() {

        return role
            == BusinessRole.OWNER
            || role
                == BusinessRole.ADMIN
            || role
                == BusinessRole.STAFF;
    }

    /**
     * Operaciones especialmente sensibles.
     *
     * Ejemplos:
     *
     * - billing
     * - subscription
     * - gestión de ADMIN
     * - transferencia futura de ownership
     */
    public boolean canManageSensitiveOperations() {

        return isOwner();
    }

    // =========================================================
    // DOMAIN HELPERS · ROLE MUTATION
    // =========================================================

    /**
     * Cambia el rol de la membership.
     *
     * IMPORTANTE:
     *
     * Este método solo valida consistencia interna.
     *
     * La autorización sobre quién puede asignar cada rol
     * pertenece a BusinessMembershipService.
     */
    public void changeRole(
        BusinessRole newRole
    ) {

        if (newRole == null) {
            throw new IllegalArgumentException(
                "newRole es obligatorio."
            );
        }

        this.role =
            newRole;
    }

    // =========================================================
    // DOMAIN HELPERS · STATUS
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
    // DOMAIN HELPERS · BUSINESS
    // =========================================================

    /**
     * Indica si esta membership pertenece al business indicado.
     */
    public boolean belongsToBusiness(
        Long businessId
    ) {

        if (
            businessId == null
                || business == null
                || business.getId() == null
        ) {
            return false;
        }

        return Objects.equals(
            business.getId(),
            businessId
        );
    }

    // =========================================================
    // DOMAIN HELPERS · USER
    // =========================================================

    /**
     * Indica si esta membership pertenece al usuario indicado.
     */
    public boolean belongsToUser(
        Long userId
    ) {

        if (
            userId == null
                || user == null
                || user.getId() == null
        ) {
            return false;
        }

        return Objects.equals(
            user.getId(),
            userId
        );
    }

    // =========================================================
    // DOMAIN HELPERS · USABILITY
    // =========================================================

    /**
     * Comprueba únicamente la consistencia básica
     * de la membership.
     *
     * No sustituye las validaciones completas de seguridad
     * de BusinessMembershipService.
     */
    public boolean isStructurallyValid() {

        return business != null
            && business.getId() != null
            && user != null
            && user.getId() != null
            && role != null;
    }

    /**
     * Una membership utilizable debe:
     *
     * - estar activa
     * - ser estructuralmente válida
     * - tener usuario activo
     * - pertenecer a un business activo
     *
     * Sigue sin sustituir la comprobación JWT/DB realizada
     * por BusinessMembershipService.
     */
    public boolean isUsable() {

        if (!active) {
            return false;
        }

        if (!isStructurallyValid()) {
            return false;
        }

        if (!user.isActive()) {
            return false;
        }

        return business.isActive();
    }
}