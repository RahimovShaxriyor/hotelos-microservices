package com.hotelos.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.identity.config.RsaKeyProvider;
import com.hotelos.identity.persistence.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Instant;
import java.util.*;

@Service
public class TokenService {

    public static final long ACCESS_TOKEN_VALIDITY_SECONDS = 900L; // 15 minutes
    public static final long WS_TICKET_VALIDITY_SECONDS = 30L;     // 30 seconds
    public static final String ISSUER = "hotelos-identity";
    public static final String AUDIENCE = "hotelos-api";
    public static final String WS_AUDIENCE = "hotelos-dashboard-ws";
    public static final String TOKEN_USE_WS_TICKET = "ws_ticket";

    private final RsaKeyProvider rsaKeyProvider;
    private final ObjectMapper objectMapper;

    public TokenService(RsaKeyProvider rsaKeyProvider, ObjectMapper objectMapper) {
        this.rsaKeyProvider = rsaKeyProvider;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(UserEntity user) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusSeconds(ACCESS_TOKEN_VALIDITY_SECONDS);

            // 1. Header
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "RS256");
            header.put("typ", "JWT");
            header.put("kid", rsaKeyProvider.getKeyId());

            // 2. Payload
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("iss", ISSUER);
            payload.put("aud", AUDIENCE);
            payload.put("sub", user.getId().toString());
            payload.put("username", user.getUsername());
            payload.put("roles", new ArrayList<>(user.getRoles()));
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", exp.getEpochSecond());
            payload.put("jti", UUID.randomUUID().toString());

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String headerBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = headerBase64 + "." + payloadBase64;

            // 3. Signature
            Signature rsaSignature = Signature.getInstance("SHA256withRSA");
            rsaSignature.initSign(rsaKeyProvider.getPrivateKey());
            rsaSignature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            byte[] signatureBytes = rsaSignature.sign();
            String signatureBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            return signingInput + "." + signatureBase64;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign RS256 access JWT", ex);
        }
    }

    public com.hotelos.identity.dto.WebSocketTicketResponse createWebSocketTicket(UUID userId, String username, List<String> roles) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusSeconds(WS_TICKET_VALIDITY_SECONDS);

            // 1. Header
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "RS256");
            header.put("typ", "JWT");
            header.put("kid", rsaKeyProvider.getKeyId());

            // 2. Payload (contractually separated from access JWT)
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("iss", ISSUER);
            payload.put("aud", WS_AUDIENCE);
            payload.put("sub", userId.toString());
            payload.put("username", username);
            payload.put("roles", new ArrayList<>(roles));
            payload.put("token_use", TOKEN_USE_WS_TICKET);
            payload.put("jti", UUID.randomUUID().toString());
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", exp.getEpochSecond());

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String headerBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = headerBase64 + "." + payloadBase64;

            // 3. Signature
            Signature rsaSignature = Signature.getInstance("SHA256withRSA");
            rsaSignature.initSign(rsaKeyProvider.getPrivateKey());
            rsaSignature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            byte[] signatureBytes = rsaSignature.sign();
            String signatureBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            String ticket = signingInput + "." + signatureBase64;
            return new com.hotelos.identity.dto.WebSocketTicketResponse(ticket, WS_TICKET_VALIDITY_SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign RS256 WebSocket ticket", ex);
        }
    }
}
