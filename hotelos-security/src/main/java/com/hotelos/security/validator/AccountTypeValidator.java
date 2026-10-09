package com.hotelos.security.validator;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

/**
 * Validates that an incoming staff access token has account_type == "STAFF".
 * Fails closed if account_type is missing, CUSTOMER, or any other value.
 */
public class AccountTypeValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAccountType;

    public AccountTypeValidator(String expectedAccountType) {
        this.expectedAccountType = Objects.requireNonNull(expectedAccountType, "expectedAccountType must not be null");
    }

    public AccountTypeValidator() {
        this("STAFF");
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object claimValue = jwt.getClaims().get("account_type");
        if (claimValue instanceof String accountTypeStr && expectedAccountType.equals(accountTypeStr)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The required account_type '" + expectedAccountType + "' is missing or invalid",
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
