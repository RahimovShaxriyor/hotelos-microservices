package com.hotelos.common.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldErrorDto(
        String field,
        String message,
        Object rejectedValue
) {
    public FieldErrorDto(String field, String message) {
        this(field, message, null);
    }
}
