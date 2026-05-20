package com.hotelos.housekeeping.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(HotelValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(HotelValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }
}
