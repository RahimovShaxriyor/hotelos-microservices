package com.hotelos.dashboard.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class WebSocketTicketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketTicketHandshakeInterceptor.class);

    private final WebSocketOriginValidator originValidator;
    private final WsTicketValidator ticketValidator;

    public WebSocketTicketHandshakeInterceptor(WebSocketOriginValidator originValidator, WsTicketValidator ticketValidator) {
        this.originValidator = originValidator;
        this.ticketValidator = ticketValidator;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {
        // 1. Origin validation (fail-closed)
        HttpHeaders headers = request.getHeaders();
        String origin = headers.getOrigin();
        if (!originValidator.isValidOrigin(origin)) {
            log.warn("WebSocket handshake rejected: untrusted or missing origin [{}]", origin);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        // 2. Extract ticket from query params
        URI uri = request.getURI();
        String query = uri.getQuery();
        String ticket = extractTicketFromQuery(query);

        if (ticket == null || ticket.isBlank()) {
            log.warn("WebSocket handshake rejected: missing ticket parameter in query");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 3. Validate and atomically consume ticket
        TicketValidationResult result = ticketValidator.validateAndConsume(ticket);
        if (result instanceof TicketValidationResult.Success success) {
            WebSocketTicketPrincipal principal = success.principal();
            // 4. Save minimal session attributes (never save raw ticket or credentials)
            attributes.put("ws_principal", principal);
            attributes.put("subject", principal.getSubject());
            attributes.put("username", principal.getUsername());
            attributes.put("roles", principal.getRoles());
            attributes.put("jti", principal.getJti().toString());

            log.info("WebSocket handshake authorized for user [{}] with jti [{}]", principal.getName(), principal.getJti());
            return true;
        } else if (result instanceof TicketValidationResult.ForbiddenRole forbidden) {
            log.warn("WebSocket handshake forbidden: unauthorized roles {}", forbidden.roles());
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        } else if (result instanceof TicketValidationResult.ServiceUnavailable unavailable) {
            log.error("WebSocket handshake unavailable due to storage failure: {}", unavailable.error());
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        } else {
            // TicketValidationResult.InvalidTicket
            log.warn("WebSocket handshake unauthorized: invalid, expired, or replayed ticket");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No-op
    }

    private String extractTicketFromQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                if ("ticket".equals(key)) {
                    return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }
}
