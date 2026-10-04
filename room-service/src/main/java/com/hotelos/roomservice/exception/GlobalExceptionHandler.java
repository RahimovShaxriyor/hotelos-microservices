package com.hotelos.roomservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(NoActiveStayException.class)
    public ResponseEntity<Map<String, Object>> handleNoActiveStay(NoActiveStayException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(Map.of("code", "NO_ACTIVE_STAY", "message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", "SERVICE_UNAVAILABLE", "message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(HotelValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(HotelValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }
}
