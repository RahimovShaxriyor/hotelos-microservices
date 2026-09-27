package com.hotelos.identity.web;

import com.hotelos.identity.dto.LoginErrorResponse;
import com.hotelos.identity.dto.LoginRequest;
import com.hotelos.identity.dto.LoginResponse;
import com.hotelos.identity.dto.LogoutRequest;
import com.hotelos.identity.dto.RefreshRequest;
import com.hotelos.identity.dto.RefreshResponse;
import com.hotelos.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Staff authentication and token operations")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Staff login", description = "Authenticate staff credentials and issue access token with refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked",
                    content = @Content(schema = @Schema(implementation = LoginErrorResponse.class)))
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        LoginResponse response = authService.login(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Rotate refresh token and issue new access token", description = "Rotate refresh token and issue new access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token rotated successfully",
                    content = @Content(schema = @Schema(implementation = RefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token",
                    content = @Content(schema = @Schema(implementation = LoginErrorResponse.class)))
    })
    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        RefreshResponse response = authService.refresh(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Revoke current login session", description = "Revoke current login session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Session revoked successfully")
    })
    @PostMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        authService.logout(request, ipAddress);
        return ResponseEntity.noContent().build();
    }
}

