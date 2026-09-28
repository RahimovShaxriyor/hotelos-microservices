package com.hotelos.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.security.converter.JwtRoleConverter;
import com.hotelos.security.validator.AudienceValidator;
import com.hotelos.security.validator.IssuerValidator;
import com.hotelos.security.validator.KidValidator;
import com.hotelos.security.web.RestAccessDeniedHandler;
import com.hotelos.security.web.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Duration;

@Configuration
public class JwtSecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RsaPublicKeyProvider rsaPublicKeyProvider(
            @Value("${hotelos.jwt.public-key-path:${HOTELOS_JWT_PUBLIC_KEY_PATH:secrets/jwt-public.pem}}") String publicKeyPath,
            @Value("${hotelos.jwt.kid:hotelos-rsa-key-1}") String keyId,
            @Value("${hotelos.jwt.issuer:hotelos-identity}") String issuer,
            @Value("${hotelos.jwt.audience:hotelos-api}") String audience
    ) {
        return new RsaPublicKeyProvider(publicKeyPath, keyId, issuer, audience);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtRoleConverter jwtRoleConverter() {
        return new JwtRoleConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(RsaPublicKeyProvider keyProvider) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(keyProvider.getPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(60));
        OAuth2TokenValidator<Jwt> issuerValidator = new IssuerValidator(keyProvider.getIssuer());
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(keyProvider.getAudience());
        OAuth2TokenValidator<Jwt> kidValidator = new KidValidator(keyProvider.getKeyId());

        OAuth2TokenValidator<Jwt> delegatingValidator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator,
                kidValidator
        );

        jwtDecoder.setJwtValidator(delegatingValidator);
        return jwtDecoder;
    }
}
