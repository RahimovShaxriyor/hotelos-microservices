package com.hotelos.security.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TYPE_PREFIX = "TYPE_";
    private static final String ROLES_CLAIM = "roles";
    private static final String ACCOUNT_TYPE_CLAIM = "account_type";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalName = jwt.getClaimAsString("username");
        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        if (roles != null) {
            roles.stream()
                    .filter(role -> role != null && !role.isBlank())
                    .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.trim()))
                    .forEach(authorities::add);
        }
        String accountType = jwt.getClaimAsString(ACCOUNT_TYPE_CLAIM);
        if (accountType != null && !accountType.isBlank()) {
            authorities.add(new SimpleGrantedAuthority(TYPE_PREFIX + accountType.trim().toUpperCase(java.util.Locale.ROOT)));
        }
        return Collections.unmodifiableList(authorities);
    }
}
