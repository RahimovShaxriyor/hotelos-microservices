package com.hotelos.reception.exception;

import com.hotelos.reception.dto.ErrorResponse;
import com.hotelos.reception.dto.ConflictErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ConflictErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictErrorResponse(ex.getCode(), ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ConflictErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ConflictErrorResponse(ex.getCode(), ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(NoActiveStayException.class)
    public ResponseEntity<ConflictErrorResponse> handleNoActiveStay(NoActiveStayException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ConflictErrorResponse("NO_ACTIVE_STAY", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(HotelValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(HotelValidationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(errors, Instant.now()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Malformed request value", Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal error. The request was not completed safely.", Instant.now()));
    }
}
