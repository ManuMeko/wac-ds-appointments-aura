package com.wacds.appointments.auth.security;

import com.wacds.appointments.auth.membership.BusinessMembership;
import com.wacds.appointments.auth.membership.BusinessMembershipRepository;
import com.wacds.appointments.auth.user.User;
import com.wacds.appointments.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
    implements UserDetailsService {

    private static final String BASE_AUTHORITY =
        "ROLE_USER";

    private final UserRepository userRepository;

    private final BusinessMembershipRepository
        businessMembershipRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(
        String username
    ) throws UsernameNotFoundException {

        String normalizedEmail =
            normalizeEmail(
                username
            );

        User user =
            userRepository
                .findByEmailIgnoreCase(
                    normalizedEmail
                )
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "No existe ningún usuario con el email indicado."
                    )
                );

        List<BusinessMembership> memberships =
            businessMembershipRepository
                .findAllByUserIdAndActiveTrueOrderByCreatedAtAsc(
                    user.getId()
                );

        List<GrantedAuthority> authorities =
            buildAuthorities(
                memberships
            );

        return org.springframework.security.core.userdetails.User
            .withUsername(
                user.getEmail()
            )
            .password(
                user.getPasswordHash()
            )
            .authorities(
                authorities
            )
            .disabled(
                !user.isActive()
            )
            .accountExpired(
                false
            )
            .accountLocked(
                false
            )
            .credentialsExpired(
                false
            )
            .build();
    }

    private List<GrantedAuthority> buildAuthorities(
        List<BusinessMembership> memberships
    ) {
        Set<String> authorityNames =
            new LinkedHashSet<>();

        authorityNames.add(
            BASE_AUTHORITY
        );

        for (
            BusinessMembership membership :
            memberships
        ) {
            if (
                membership == null
                    || !membership.isActive()
                    || membership.getRole() == null
            ) {
                continue;
            }

            authorityNames.add(
                "ROLE_"
                    + membership
                        .getRole()
                        .name()
            );
        }

        List<GrantedAuthority> authorities =
            new ArrayList<>();

        for (
            String authorityName :
            authorityNames
        ) {
            authorities.add(
                new SimpleGrantedAuthority(
                    authorityName
                )
            );
        }

        return authorities;
    }

    private String normalizeEmail(
        String email
    ) {
        if (
            email == null
                || email.isBlank()
        ) {
            throw new UsernameNotFoundException(
                "El email es obligatorio."
            );
        }

        return email
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }
}