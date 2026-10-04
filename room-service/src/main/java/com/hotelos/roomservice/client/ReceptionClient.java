package com.hotelos.roomservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.roomservice.dto.ActiveStayResolutionResponse;
import com.hotelos.roomservice.exception.ConflictException;
import com.hotelos.roomservice.exception.HotelValidationException;
import com.hotelos.roomservice.exception.NoActiveStayException;
import com.hotelos.roomservice.exception.ServiceUnavailableException;
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
public class ReceptionClient {

    private static final Logger log = LoggerFactory.getLogger(ReceptionClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ReceptionClient(
            @Value("${hotelos.reception.service-url:${RECEPTION_SERVICE_URL:http://localhost:8081}}") String receptionUrl,
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
                .baseUrl(receptionUrl)
                .requestFactory(new JdkClientHttpRequestFactory())
                .requestInterceptor(authInterceptor)
                .build();
    }

    public ActiveStayResolutionResponse resolveActiveStay(String roomNumber) {
        try {
            return restClient.get()
                    .uri("/api/reception/internal/active-stays/by-room/{roomNumber}", roomNumber)
                    .retrieve()
                    .body(ActiveStayResolutionResponse.class);
        } catch (HttpClientErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            String code = extractCode(responseBody);
            String message = extractMessage(responseBody);

            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Active stay not found for room {}: {}", roomNumber, responseBody);
                throw new NoActiveStayException(message.isEmpty() ? "No active stay for room " + roomNumber : message);
            }
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                log.warn("Conflict resolving stay for room {}: {}", roomNumber, responseBody);
                throw new ConflictException(code.isEmpty() ? "INTEGRITY_ANOMALY" : code, message);
            }
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new HotelValidationException(message.isEmpty() ? "Invalid request to reception" : message);
            }
            throw new ServiceUnavailableException("Error communicating with Reception: " + ex.getMessage(), ex);
        } catch (HttpServerErrorException ex) {
            log.error("Reception service returned server error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ServiceUnavailableException("Reception service returned error: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            log.error("Reception service unavailable: {}", ex.getMessage());
            throw new ServiceUnavailableException("Reception service unavailable", ex);
        } catch (Exception ex) {
            log.error("Unexpected error calling Reception: {}", ex.getMessage());
            throw new ServiceUnavailableException("Reception communication failure: " + ex.getMessage(), ex);
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
