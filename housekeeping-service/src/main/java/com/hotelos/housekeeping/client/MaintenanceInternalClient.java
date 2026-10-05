package com.hotelos.housekeeping.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.housekeeping.exception.ConflictException;
import com.hotelos.housekeeping.exception.HotelValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Component
public class MaintenanceInternalClient {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceInternalClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MaintenanceInternalClient(
            @Value("${MAINTENANCE_SERVICE_URL:http://localhost:8084}") String maintenanceServiceUrl,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest servletRequest = servletAttributes.getRequest();
                String authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
                }
            }
            return execution.execute(request, body);
        };

        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory())
                .baseUrl(maintenanceServiceUrl)
                .requestInterceptor(authInterceptor)
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createInternalIssue(String idempotencyKey, Map<String, Object> requestBody) {
        try {
            return restClient.post()
                    .uri("/api/maintenance/internal/issues")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException ex) {
            String bodyStr = ex.getResponseBodyAsString();
            log.warn("Maintenance internal issue call failed with status {}: {}", ex.getStatusCode(), bodyStr);
            String code = null;
            String message = ex.getMessage();
            try {
                if (bodyStr != null && !bodyStr.isBlank()) {
                    JsonNode node = objectMapper.readTree(bodyStr);
                    if (node.has("code")) {
                        code = node.get("code").asText();
                    }
                    if (node.has("message")) {
                        message = node.get("message").asText();
                    }
                }
            } catch (Exception ignored) {
            }

            if (ex.getStatusCode().value() == 409) {
                throw new ConflictException(code != null ? code : "IDEMPOTENCY_CONFLICT", message);
            }
            if (ex.getStatusCode().value() == 400) {
                throw new HotelValidationException(message);
            }
            throw ex;
        }
    }
}
