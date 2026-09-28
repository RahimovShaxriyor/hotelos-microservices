package com.hotelos.dashboard.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class WebSocketOriginValidator {

    private static final Logger log = LoggerFactory.getLogger(WebSocketOriginValidator.class);

    private final List<String> allowedOrigins;

    public WebSocketOriginValidator(
            @Value("${hotelos.ws.allowed-origins:http://localhost:8085,http://localhost:8090,http://127.0.0.1:8085,http://127.0.0.1:8090}")
            String allowedOriginsStr
    ) {
        this.allowedOrigins = Arrays.stream(allowedOriginsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Strictly validates the Origin header.
     * Missing or empty Origin is rejected (fail-closed policy: only trusted web clients with matching Origin).
     */
    public boolean isValidOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            log.warn("WebSocket handshake rejected: Origin header is missing or empty");
            return false;
        }

        String normalized = origin.trim();
        for (String allowed : allowedOrigins) {
            if (allowed.equalsIgnoreCase(normalized)) {
                return true;
            }
        }

        log.warn("WebSocket handshake rejected: Origin '{}' is not in trusted origins {}", origin, allowedOrigins);
        return false;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }
}
