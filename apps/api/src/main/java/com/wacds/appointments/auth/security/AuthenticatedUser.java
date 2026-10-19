package com.wacds.appointments.auth.security;

import com.wacds.appointments.auth.membership.BusinessRole;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class AuthenticatedUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;

    private final String email;

    private final Long businessId;

    private final BusinessRole role;

    public AuthenticatedUser(
        Long userId,
        String email,
        Long businessId,
        BusinessRole role
    ) {
        this.userId =
            Objects.requireNonNull(
                userId,
                "userId no puede ser null"
            );

        this.email =
            Objects.requireNonNull(
                email,
                "email no puede ser null"
            );

        this.businessId =
            Objects.requireNonNull(
                businessId,
                "businessId no puede ser null"
            );

        this.role =
            Objects.requireNonNull(
                role,
                "role no puede ser null"
            );
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public BusinessRole getRole() {
        return role;
    }

    public boolean isOwner() {
        return role == BusinessRole.OWNER;
    }

    public boolean isAdmin() {
        return role == BusinessRole.ADMIN;
    }

    public boolean isStaff() {
        return role == BusinessRole.STAFF;
    }

    public boolean canManageBusiness() {
        return role == BusinessRole.OWNER
            || role == BusinessRole.ADMIN;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{" +
            "userId=" + userId +
            ", email='" + email + '\'' +
            ", businessId=" + businessId +
            ", role=" + role +
            '}';
    }
}