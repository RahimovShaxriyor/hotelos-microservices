package com.hotelos.identity.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public enum GovernanceAuthority {
    OWNER,
    ADMIN,
    NONE;

    public static GovernanceAuthority fromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return NONE;
        }
        Set<String> authNames = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (authNames.contains("ROLE_OWNER")) {
            return OWNER;
        }
        if (authNames.contains("ROLE_ADMIN")) {
            return ADMIN;
        }
        return NONE;
    }

    public static GovernanceAuthority fromRoles(Set<String> roles) {
        if (roles == null) {
            return NONE;
        }
        if (roles.contains("OWNER")) {
            return OWNER;
        }
        if (roles.contains("ADMIN")) {
            return ADMIN;
        }
        return NONE;
    }

    public static GovernanceAuthority fromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return NONE;
        }
        return fromAuthorities(authentication.getAuthorities());
    }
}
