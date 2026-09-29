package com.hotelos.common.currency;

import java.util.Currency;

public final class CurrencyValidator {

    public static final String DEFAULT_CURRENCY = "UZS";

    private CurrencyValidator() {}

    public static String validateAndNormalize(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().length() != 3) {
            throw new IllegalArgumentException("Currency code must be exactly 3 uppercase letters");
        }
        String normalized = currencyCode.trim().toUpperCase();
        if (!normalized.equals(currencyCode)) {
            throw new IllegalArgumentException("Currency code must be uppercase: " + currencyCode);
        }
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ISO-4217 currency code: " + currencyCode);
        }
        return normalized;
    }

    public static void validate(String currencyCode) {
        validateAndNormalize(currencyCode);
    }
}
