package com.hotelos.reception.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.reception.dto.FinalizeStayRequest;
import com.hotelos.reception.dto.FreezeStayResponse;
import com.hotelos.reception.dto.UnfreezeStayRequest;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.exception.ServiceUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class RoomServiceInternalClient {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceInternalClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RoomServiceInternalClient(
            @Value("${hotelos.room-service.service-url:${ROOM_SERVICE_URL:http://localhost:8083}}") String roomServiceUrl,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
                if (attributes instanceof ServletRequestAttributes servletAttributes) {
                    HttpServletRequest servletRequest = servletAttributes.getRequest();
                    String authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
                    }
                }
            }
            return execution.execute(request, body);
        };

        this.restClient = RestClient.builder()
                .baseUrl(roomServiceUrl)
                .requestFactory(new JdkClientHttpRequestFactory())
                .requestInterceptor(authInterceptor)
                .build();
    }

    public FreezeStayResponse freezeStay(UUID stayId) {
        try {
            return restClient.post()
                    .uri("/api/room-service/internal/stays/{stayId}/freeze", stayId)
                    .retrieve()
                    .body(FreezeStayResponse.class);
        } catch (HttpClientErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            String code = extractCode(responseBody);
            String message = extractMessage(responseBody);

            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                log.warn("Conflict freezing stay {}: code={}, msg={}", stayId, code, message);
                throw new ConflictException(code.isEmpty() ? "FREEZE_CONFLICT" : code, message);
            }
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(message.isEmpty() ? "Stay ordering session not found: " + stayId : message);
            }
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new HotelValidationException(message.isEmpty() ? "Invalid request to room-service" : message);
            }
            throw new ServiceUnavailableException("ROOM_SERVICE_UNAVAILABLE", "Error communicating with Room Service: " + ex.getMessage(), ex);
        } catch (HttpServerErrorException ex) {
            log.error("Room service returned server error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ServiceUnavailableException("ROOM_SERVICE_UNAVAILABLE", "Room service returned error: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            log.error("Room service unavailable during freeze for stay {}: {}", stayId, ex.getMessage());
            throw new ServiceUnavailableException("ROOM_SERVICE_UNAVAILABLE", "Room service unavailable: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during freeze for stay {}: {}", stayId, ex.getMessage());
            throw new ServiceUnavailableException("ROOM_SERVICE_UNAVAILABLE", "Room service communication failure: " + ex.getMessage(), ex);
        }
    }

    public void finalizeStay(UUID stayId, UUID freezeId) {
        try {
            restClient.post()
                    .uri("/api/room-service/internal/stays/{stayId}/finalize", stayId)
                    .body(new FinalizeStayRequest(freezeId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            String code = extractCode(responseBody);
            String message = extractMessage(responseBody);

            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                log.warn("Conflict finalizing stay {}: code={}, msg={}", stayId, code, message);
                throw new ConflictException(code.isEmpty() ? "FINALIZE_CONFLICT" : code, message);
            }
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(message.isEmpty() ? "Stay ordering session not found: " + stayId : message);
            }
            throw new ServiceUnavailableException("ROOM_SERVICE_UNAVAILABLE", "Error communicating with Room Service: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.warn("Error finalizing stay {} with freezeId {}: {}", stayId, freezeId, ex.getMessage());
            throw new ServiceUnavailableException("ROOM_SERVICE_UNAVAILABLE", "Room service finalize error: " + ex.getMessage(), ex);
        }
    }

    public void unfreezeStay(UUID stayId, UUID freezeId) {
        try {
            restClient.post()
                    .uri("/api/room-service/internal/stays/{stayId}/unfreeze", stayId)
                    .body(new UnfreezeStayRequest(freezeId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Best effort unfreeze failed for stay {} with freezeId {}: {}", stayId, freezeId, ex.getMessage());
        }
    }

    private String extractCode(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("code")) {
                return node.get("code").asText();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String extractMessage(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
