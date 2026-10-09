package com.hotelos.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.dto.error.ApiErrorResponse;
import com.hotelos.security.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);
    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Forbidden request to [{}]: {}", request.getRequestURI(), accessDeniedException.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        if (correlationId == null) {
            correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        }

        ApiErrorResponse body = ApiErrorResponse.of(
                "FORBIDDEN",
                403,
                "Access denied",
                "Forbidden",
                correlationId,
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
