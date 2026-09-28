package com.hotelos.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaPublicKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaPublicKeyProvider.class);

    private final RSAPublicKey publicKey;
    private final String keyId;
    private final String issuer;
    private final String audience;

    public RsaPublicKeyProvider(
            @Value("${hotelos.jwt.public-key-path:${HOTELOS_JWT_PUBLIC_KEY_PATH:secrets/jwt-public.pem}}") String publicKeyPath,
            @Value("${hotelos.jwt.kid:hotelos-rsa-key-1}") String keyId,
            @Value("${hotelos.jwt.issuer:hotelos-identity}") String issuer,
            @Value("${hotelos.jwt.audience:hotelos-api}") String audience
    ) {
        this.keyId = keyId;
        this.issuer = issuer;
        this.audience = audience;
        this.publicKey = loadPublicKey(publicKeyPath);
        log.info("RSA public key provider initialized for key-id [{}]", keyId);
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    private RSAPublicKey loadPublicKey(String pathStr) {
        try {
            String pem = readKeyContent(pathStr);
            String cleanPem = cleanPem(pem);
            byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load RSA public key from: " + pathStr + ". Ensure X.509 PEM format exists.", ex);
        }
    }

    private String readKeyContent(String pathStr) throws Exception {
        Path path = Path.of(pathStr);
        if (Files.exists(path)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        Path altPath = Path.of("/" + pathStr.replaceAll("^/+", ""));
        if (Files.exists(altPath)) {
            return Files.readString(altPath, StandardCharsets.UTF_8);
        }
        Path parentPath = Path.of("..", pathStr);
        if (Files.exists(parentPath)) {
            return Files.readString(parentPath, StandardCharsets.UTF_8);
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pathStr)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("Public key file not found at: " + pathStr);
    }

    private String cleanPem(String pem) {
        return pem.replaceAll("-----BEGIN [A-Z0-9 ]+-----", "")
                .replaceAll("-----END [A-Z0-9 ]+-----", "")
                .replaceAll("\\s+", "");
    }
}
