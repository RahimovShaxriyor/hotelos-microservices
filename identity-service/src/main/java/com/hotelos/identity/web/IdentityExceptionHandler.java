package com.hotelos.identity.web;

import com.hotelos.common.dto.error.ApiErrorResponse;
import com.hotelos.common.dto.error.FieldErrorDto;
import com.hotelos.identity.dto.LoginErrorResponse;
import com.hotelos.identity.exception.BadCredentialsException;
import com.hotelos.identity.exception.IdentityBusinessException;
import com.hotelos.identity.exception.InvalidRefreshTokenException;
import com.hotelos.security.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class IdentityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IdentityExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<LoginErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new LoginErrorResponse("Unauthorized", "Invalid username or password"));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<LoginErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new LoginErrorResponse("Unauthorized", "Invalid refresh token"));
    }

    @ExceptionHandler(IdentityBusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleIdentityBusinessException(IdentityBusinessException ex, HttpServletRequest request) {
        log.warn("Identity business exception on [{}]: code={} msg={}", request.getRequestURI(), ex.getCode(), ex.getMessage());
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiErrorResponse body = ApiErrorResponse.of(
                ex.getCode(),
                ex.getStatus().value(),
                ex.getMessage(),
                ex.getStatus().getReasonPhrase(),
                correlationId,
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on [{}]: {}", request.getRequestURI(), ex.getMessage());
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiErrorResponse body = ApiErrorResponse.of(
                "ACCESS_DENIED",
                HttpStatus.FORBIDDEN.value(),
                "Access is denied",
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                correlationId,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        List<FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ApiErrorResponse body = ApiErrorResponse.of(
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed for request",
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                correlationId,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument on [{}]: {}", request.getRequestURI(), ex.getMessage());
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiErrorResponse body = ApiErrorResponse.of(
                "ROLE_NOT_ASSIGNABLE",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                correlationId,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on [{}]:", request.getRequestURI(), ex);
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        ApiErrorResponse body = ApiErrorResponse.of(
                "INTERNAL_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                correlationId,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
