package com.hotelos.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;

    public RsaKeyProvider(
            @Value("${hotelos.jwt.private-key-path:secrets/jwt-private.pem}") String privateKeyPath,
            @Value("${hotelos.jwt.public-key-path:secrets/jwt-public.pem}") String publicKeyPath,
            @Value("${hotelos.jwt.kid:hotelos-rsa-key-1}") String keyId
    ) {
        this.keyId = keyId;
        this.privateKey = loadPrivateKey(privateKeyPath);
        this.publicKey = loadPublicKey(publicKeyPath);
        log.info("RSA key provider initialized successfully with key-id: [{}]", keyId);
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }

    private RSAPrivateKey loadPrivateKey(String pathStr) {
        try {
            String pem = readKeyContent(pathStr);
            String cleanPem = cleanPem(pem);
            byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load RSA private key from: " + pathStr + ". Ensure PKCS#8 PEM format exists.", ex);
        }
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
        // Try fallback to /secrets/ path
        Path altPath = Path.of("/" + pathStr.replaceAll("^/+", ""));
        if (Files.exists(altPath)) {
            return Files.readString(altPath, StandardCharsets.UTF_8);
        }
        // Try fallback to parent directory (e.g. running from identity-service submodule directory)
        Path parentPath = Path.of("..", pathStr);
        if (Files.exists(parentPath)) {
            return Files.readString(parentPath, StandardCharsets.UTF_8);
        }
        // Try classpath resource
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pathStr)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("Key file not found at: " + pathStr + " or /" + pathStr);
    }

    private String cleanPem(String pem) {
        return pem.replaceAll("-----BEGIN [A-Z0-9 ]+-----", "")
                .replaceAll("-----END [A-Z0-9 ]+-----", "")
                .replaceAll("\\s+", "");
    }
}
