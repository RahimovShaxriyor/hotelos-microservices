package com.hotelos.security.validator;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class KidValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedKeyId;

    public KidValidator(String expectedKeyId) {
        this.expectedKeyId = expectedKeyId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object kidObj = jwt.getHeaders().get("kid");
        if (kidObj != null && expectedKeyId.equals(kidObj.toString())) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The token key id (kid) is invalid. Expected: " + expectedKeyId,
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
