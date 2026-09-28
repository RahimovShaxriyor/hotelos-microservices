package com.hotelos.dashboard.websocket;

import com.hotelos.dashboard.repository.WsTicketConsumptionRepository;
import com.hotelos.security.config.RsaPublicKeyProvider;
import com.hotelos.security.validator.AudienceValidator;
import com.hotelos.security.validator.IssuerValidator;
import com.hotelos.security.validator.KidValidator;
import com.hotelos.security.validator.TokenUseValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class WsTicketValidator {

    private static final Logger log = LoggerFactory.getLogger(WsTicketValidator.class);
    private static final String WS_AUDIENCE = "hotelos-dashboard-ws";
    private static final String WS_TOKEN_USE = "ws_ticket";

    private final JwtDecoder wsTicketDecoder;
    private final WsTicketConsumptionRepository consumptionRepository;

    public WsTicketValidator(RsaPublicKeyProvider keyProvider, WsTicketConsumptionRepository consumptionRepository) {
        this.consumptionRepository = consumptionRepository;

        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keyProvider.getPublicKey())
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();

            OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(10));
            OAuth2TokenValidator<Jwt> issuerValidator = new IssuerValidator(keyProvider.getIssuer());
            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(WS_AUDIENCE);
            OAuth2TokenValidator<Jwt> kidValidator = new KidValidator(keyProvider.getKeyId());
            OAuth2TokenValidator<Jwt> tokenUseValidator = new TokenUseValidator(WS_TOKEN_USE);

            OAuth2TokenValidator<Jwt> delegatingValidator = new DelegatingOAuth2TokenValidator<>(
                    timestampValidator,
                    issuerValidator,
                    audienceValidator,
                    kidValidator,
                    tokenUseValidator
            );

            decoder.setJwtValidator(delegatingValidator);
            this.wsTicketDecoder = decoder;
            log.info("WebSocket ticket validator initialized successfully for audience [{}]", WS_AUDIENCE);
        } catch (Exception ex) {
            log.error("Failed to initialize WebSocket ticket validator: {}", ex.getMessage());
            throw new IllegalStateException("WebSocket ticket validator initialization failed (fail-closed)", ex);
        }
    }

    /**
     * Validates the raw ticket string and atomically consumes it if valid.
     * Returns TicketValidationResult with explicit semantics (Success, InvalidTicket, ForbiddenRole, ServiceUnavailable).
     */
    public TicketValidationResult validateAndConsume(String rawTicket) {
        if (rawTicket == null || rawTicket.isBlank()) {
            log.warn("WebSocket ticket validation failed: ticket is missing or empty");
            return new TicketValidationResult.InvalidTicket("missing ticket");
        }

        Jwt jwt;
        try {
            jwt = wsTicketDecoder.decode(rawTicket);
        } catch (JwtException ex) {
            log.warn("WebSocket ticket JWT validation failed: {}", ex.getMessage());
            return new TicketValidationResult.InvalidTicket(ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error decoding WebSocket ticket: {}", ex.getMessage());
            return new TicketValidationResult.InvalidTicket(ex.getMessage());
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            log.warn("WebSocket ticket validation failed: missing subject");
            return new TicketValidationResult.InvalidTicket("missing subject");
        }

        String jtiStr = jwt.getId();
        if (jtiStr == null || jtiStr.isBlank()) {
            log.warn("WebSocket ticket validation failed: missing jti claim");
            return new TicketValidationResult.InvalidTicket("missing jti claim");
        }

        UUID jti;
        try {
            jti = UUID.fromString(jtiStr);
        } catch (IllegalArgumentException e) {
            log.warn("WebSocket ticket validation failed: invalid jti UUID [{}]", jtiStr);
            return new TicketValidationResult.InvalidTicket("invalid jti UUID");
        }

        Instant notBefore = jwt.getNotBefore();
        if (notBefore != null && notBefore.isAfter(Instant.now().plus(Duration.ofSeconds(10)))) {
            log.warn("WebSocket ticket validation failed: future nbf [{}]", notBefore);
            return new TicketValidationResult.InvalidTicket("future nbf");
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.isEmpty()) {
            log.warn("WebSocket ticket validation failed: no roles claim present");
            return new TicketValidationResult.ForbiddenRole(java.util.Collections.emptyList());
        }

        boolean hasAuthorizedRole = roles.contains("ADMIN") || roles.contains("MANAGER");
        if (!hasAuthorizedRole) {
            log.warn("WebSocket ticket validation failed: unauthorized roles {}", roles);
            return new TicketValidationResult.ForbiddenRole(roles);
        }

        String username = jwt.getClaimAsString("username");
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            log.warn("WebSocket ticket validation failed: missing exp claim");
            return new TicketValidationResult.InvalidTicket("missing exp claim");
        }

        // Atomic anti-replay consumption in PostgreSQL
        try {
            boolean consumed = consumptionRepository.consumeTicket(jti, sub, username, expiresAt);
            if (!consumed) {
                log.warn("WebSocket ticket replay detected: jti={}", jti);
                return new TicketValidationResult.InvalidTicket("ticket already consumed (replay)");
            }
        } catch (Exception ex) {
            log.error("WebSocket ticket consumption failed due to database error (fail-closed): {}", ex.getMessage());
            return new TicketValidationResult.ServiceUnavailable(ex.getMessage());
        }

        return new TicketValidationResult.Success(new WebSocketTicketPrincipal(sub, username, roles, jti));
    }
}
