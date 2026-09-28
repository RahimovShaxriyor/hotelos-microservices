package com.hotelos.security.validator;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class TokenUseValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedTokenUse;

    public TokenUseValidator(String expectedTokenUse) {
        this.expectedTokenUse = expectedTokenUse;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenUse = jwt.getClaimAsString("token_use");
        if (expectedTokenUse.equals(tokenUse)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The token_use claim must be '" + expectedTokenUse + "'",
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
