package com.hotelos.gateway.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class GatewayExceptionHandler {
    private final ObjectMapper objectMapper;

    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<JsonNode> handleDownstreamHttpError(RestClientResponseException ex) {
        JsonNode body = parseOrFallback(ex.getResponseBodyAsString(), ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<JsonNode> handleServiceUnavailable(ResourceAccessException ex) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("error", "Downstream service is unavailable");
        body.put("message", "The gateway could not connect to one of the HotelOS services.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonNode> handleUnexpected(Exception ex) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("error", "Gateway error");
        body.put("message", "The request could not be processed safely.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private JsonNode parseOrFallback(String rawBody, String fallbackMessage) {
        try {
            if (rawBody != null && !rawBody.isBlank()) {
                return objectMapper.readTree(rawBody);
            }
        } catch (Exception ignored) {
            // Return a safe structured response instead of exposing parser details.
        }
        ObjectNode fallback = objectMapper.createObjectNode();
        fallback.put("error", "Downstream service error");
        fallback.put("message", fallbackMessage == null ? "Unknown downstream error" : fallbackMessage);
        return fallback;
    }
}
