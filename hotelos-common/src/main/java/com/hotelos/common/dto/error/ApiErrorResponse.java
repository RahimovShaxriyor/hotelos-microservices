package com.hotelos.common.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String code,
        Integer status,
        String message,
        String error,
        String correlationId,
        Instant timestamp,
        String path,
        List<FieldErrorDto> fieldErrors
) {
    public static ApiErrorResponse of(String code, Integer status, String message, String error, String correlationId, String path) {
        return new ApiErrorResponse(code, status, message, error, correlationId, Instant.now(), path, null);
    }

    public static ApiErrorResponse of(String code, Integer status, String message, String error, String correlationId, String path, List<FieldErrorDto> fieldErrors) {
        return new ApiErrorResponse(code, status, message, error, correlationId, Instant.now(), path, fieldErrors);
    }

    public static ApiErrorResponse of(String code, String message, String error, String correlationId, String path) {
        return new ApiErrorResponse(code, null, message, error, correlationId, Instant.now(), path, null);
    }
}
