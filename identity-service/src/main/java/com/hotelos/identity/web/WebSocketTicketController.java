package com.hotelos.identity.web;

import com.hotelos.identity.dto.WebSocketTicketResponse;
import com.hotelos.identity.service.AuthAuditLogService;
import com.hotelos.identity.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Tag(name = "WebSocket Tickets", description = "One-time ticket generation for real-time WebSocket connections")
@RestController
@RequestMapping("/api/ws/tickets")
public class WebSocketTicketController {

    private final TokenService tokenService;
    private final AuthAuditLogService auditLogService;

    public WebSocketTicketController(TokenService tokenService, AuthAuditLogService auditLogService) {
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Issue a one-time short-lived WebSocket ticket for Dashboard")
    @PostMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebSocketTicketResponse> issueDashboardTicket(
            Authentication authentication,
            HttpServletRequest request
    ) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String subStr = jwt.getSubject();
            if (subStr == null || subStr.isBlank()) {
                throw new AccessDeniedException("Subject missing in access JWT");
            }
            UUID userId;
            try {
                userId = UUID.fromString(subStr);
            } catch (IllegalArgumentException e) {
                throw new AccessDeniedException("Subject in access JWT must be a valid UUID");
            }

            String username = jwt.getClaimAsString("username");
            if (username == null || username.isBlank()) {
                username = jwt.getSubject();
            }

            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                roles = Collections.emptyList();
            }

            WebSocketTicketResponse response = tokenService.createWebSocketTicket(userId, username, roles);
            auditLogService.recordEventIndependent("WS_TICKET_ISSUED", userId, username, request.getRemoteAddr(), "Dashboard WebSocket ticket issued");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(response);
        }
        throw new AccessDeniedException("Authentication required to issue WebSocket ticket");
    }
}
