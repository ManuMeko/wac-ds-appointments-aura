package com.wacds.appointments.auth.membership;

import com.wacds.appointments.auth.membership.dto.BusinessMembershipResponse;
import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.business.Business;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BusinessMembershipMapper {

    // =========================================================
    // TO RESPONSE
    // =========================================================

    public BusinessMembershipResponse toResponse(
        BusinessMembership membership
    ) {

        if (membership == null) {
            throw new IllegalArgumentException(
                "membership es obligatorio."
            );
        }

        Business business =
            membership.getBusiness();

        User user =
            membership.getUser();

        if (
            business == null
                || business.getId() == null
        ) {
            throw new IllegalStateException(
                "La membresía no tiene un negocio válido asociado."
            );
        }

        if (
            user == null
                || user.getId() == null
        ) {
            throw new IllegalStateException(
                "La membresía no tiene un usuario válido asociado."
            );
        }

        if (membership.getRole() == null) {
            throw new IllegalStateException(
                "La membresía no tiene un rol válido."
            );
        }

        return new BusinessMembershipResponse(
            membership.getId(),
            business.getId(),
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            membership.getRole(),
            membership.isActive(),
            user.isActive(),
            user.isEmailVerified(),
            user.getLastLoginAt(),
            membership.getCreatedAt(),
            membership.getUpdatedAt()
        );
    }

    // =========================================================
    // TO RESPONSE LIST
    // =========================================================

    public List<BusinessMembershipResponse> toResponseList(
        List<BusinessMembership> memberships
    ) {

        if (
            memberships == null
                || memberships.isEmpty()
        ) {
            return List.of();
        }

        return memberships
            .stream()
            .map(
                this::toResponse
            )
            .toList();
    }
}