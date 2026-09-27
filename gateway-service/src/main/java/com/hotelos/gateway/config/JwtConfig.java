package com.hotelos.gateway.config;

import com.hotelos.gateway.security.AudienceValidator;
import com.hotelos.gateway.security.IssuerValidator;
import com.hotelos.gateway.security.KidValidator;
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
public class JwtConfig {

    @Bean
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
