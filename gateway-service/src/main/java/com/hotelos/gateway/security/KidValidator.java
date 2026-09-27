package com.hotelos.gateway.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class KidValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedKid;

    public KidValidator(String expectedKid) {
        this.expectedKid = expectedKid;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object kid = jwt.getHeaders().get("kid");
        if (expectedKid != null && expectedKid.equals(kid)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error("invalid_token", "The token key ID (kid) is invalid", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
