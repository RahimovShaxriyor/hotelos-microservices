package com.hotelos.identity.web;

import com.hotelos.identity.dto.LoginErrorResponse;
import com.hotelos.identity.exception.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IdentityExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<LoginErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new LoginErrorResponse("Unauthorized", "Invalid username or password"));
    }

    @ExceptionHandler(com.hotelos.identity.exception.InvalidRefreshTokenException.class)
    public ResponseEntity<LoginErrorResponse> handleInvalidRefreshToken(com.hotelos.identity.exception.InvalidRefreshTokenException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new LoginErrorResponse("Unauthorized", "Invalid refresh token"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LoginErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new LoginErrorResponse("Internal Server Error", "An unexpected error occurred"));
    }
}
