package com.hotelos.gateway.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class IssuerValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedIssuer;

    public IssuerValidator(String expectedIssuer) {
        this.expectedIssuer = expectedIssuer;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getClaimAsString("iss");
        if (expectedIssuer != null && expectedIssuer.equals(issuer)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error("invalid_token", "The token issuer is invalid", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
