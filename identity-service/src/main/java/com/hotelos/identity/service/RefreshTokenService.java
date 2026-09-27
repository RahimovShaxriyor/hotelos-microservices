package com.hotelos.identity.service;

import com.hotelos.identity.domain.UserStatus;
import com.hotelos.identity.dto.RefreshResponse;
import com.hotelos.identity.exception.InvalidRefreshTokenException;
import com.hotelos.identity.persistence.entity.RefreshSessionEntity;
import com.hotelos.identity.persistence.entity.UserEntity;
import com.hotelos.identity.persistence.repository.RefreshSessionRepository;
import com.hotelos.identity.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public static final Duration REFRESH_FAMILY_LIFETIME = Duration.ofHours(8); // 28800 seconds

    private final RefreshSessionRepository refreshSessionRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthAuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public record RefreshTokenDetails(String rawRefreshToken, long refreshExpiresIn) {}

    private static class RefreshExecutionResult {
        private final boolean success;
        private final RefreshResponse response;

        private RefreshExecutionResult(boolean success, RefreshResponse response) {
            this.success = success;
            this.response = response;
        }

        public static RefreshExecutionResult success(RefreshResponse response) {
            return new RefreshExecutionResult(true, response);
        }

        public static RefreshExecutionResult invalid() {
            return new RefreshExecutionResult(false, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public RefreshResponse getResponse() {
            return response;
        }
    }

    public RefreshTokenService(
            RefreshSessionRepository refreshSessionRepository,
            UserRepository userRepository,
            TokenService tokenService,
            AuthAuditLogService auditLogService,
            PlatformTransactionManager transactionManager
    ) {
        this.refreshSessionRepository = refreshSessionRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public String generateRawRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    public RefreshTokenDetails createInitialSession(UUID userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(REFRESH_FAMILY_LIFETIME);
        UUID familyId = UUID.randomUUID();

        String rawToken = generateRawRefreshToken();
        String tokenHash = hashToken(rawToken);

        RefreshSessionEntity session = new RefreshSessionEntity(
                UUID.randomUUID(),
                userId,
                familyId,
                tokenHash,
                now,
                expiresAt,
                now,
                null
        );
        refreshSessionRepository.save(session);

        long refreshExpiresIn = Math.max(0, Duration.between(now, expiresAt).toSeconds());
        return new RefreshTokenDetails(rawToken, refreshExpiresIn);
    }

    public RefreshResponse rotateRefreshToken(String rawRefreshToken, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            auditLogService.recordEventIndependent("REFRESH_FAILURE", null, "unknown", ipAddress, "Refresh failed: empty token");
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String tokenHash = hashToken(rawRefreshToken);

        RefreshExecutionResult result = transactionTemplate.execute(status -> {
            // 1. SELECT R1 FOR UPDATE
            Optional<RefreshSessionEntity> sessionOpt = refreshSessionRepository.findByTokenHashForUpdate(tokenHash);

            if (sessionOpt.isEmpty()) {
                auditLogService.recordEventIndependent("REFRESH_FAILURE", null, "unknown", ipAddress, "Refresh failed: token not found");
                return RefreshExecutionResult.invalid();
            }

            RefreshSessionEntity session = sessionOpt.get();
            Optional<UserEntity> userOpt = userRepository.findById(session.getUserId());

            if (userOpt.isEmpty()) {
                auditLogService.recordEventIndependent("REFRESH_FAILURE", session.getUserId(), "unknown", ipAddress, "Refresh failed: user not found");
                return RefreshExecutionResult.invalid();
            }

            UserEntity user = userOpt.get();
            Instant now = Instant.now();

            // 2. Check user active status: if disabled, revoke all active sessions for this user and commit
            if (user.getStatus() == UserStatus.DISABLED) {
                refreshSessionRepository.revokeAllForUser(user.getId(), now);
                auditLogService.recordEventIndependent("REFRESH_FAILURE", user.getId(), user.getUsername(), ipAddress,
                        "Refresh failed: user disabled, all sessions revoked");
                return RefreshExecutionResult.invalid();
            }

            // 3. Check reuse: if token is already revoked, revoke entire family and commit
            if (session.getRevokedAt() != null) {
                refreshSessionRepository.revokeFamily(session.getFamilyId(), now);
                auditLogService.recordEventIndependent("REFRESH_REUSE_DETECTED", user.getId(), user.getUsername(), ipAddress,
                        "Refresh token reuse detected: family " + session.getFamilyId() + " revoked");
                return RefreshExecutionResult.invalid();
            }

            // 4. Check expiration: must not be expired
            if (!session.getExpiresAt().isAfter(now)) {
                auditLogService.recordEventIndependent("REFRESH_FAILURE", user.getId(), user.getUsername(), ipAddress,
                        "Refresh failed: token expired");
                return RefreshExecutionResult.invalid();
            }

            // 5. Mark old session revoked and update last_used_at
            session.setRevokedAt(now);
            session.setLastUsedAt(now);
            refreshSessionRepository.save(session);

            // 6. Generate and persist new session in SAME family with SAME absolute expires_at
            String newRawToken = generateRawRefreshToken();
            String newTokenHash = hashToken(newRawToken);

            RefreshSessionEntity newSession = new RefreshSessionEntity(
                    UUID.randomUUID(),
                    user.getId(),
                    session.getFamilyId(),
                    newTokenHash,
                    now,
                    session.getExpiresAt(),
                    now,
                    null
            );
            refreshSessionRepository.save(newSession);

            // 7. Generate and sign access JWT within the SAME transaction!
            // If this throws an exception, transactionTemplate catches it and rolls back DB changes!
            String accessToken = tokenService.createAccessToken(user);

            // 8. Write REFRESH_SUCCESS audit
            auditLogService.recordEventIndependent("REFRESH_SUCCESS", user.getId(), user.getUsername(), ipAddress,
                    "Refresh token rotated successfully for family " + session.getFamilyId());

            long remainingSeconds = Math.max(0, Duration.between(now, session.getExpiresAt()).toSeconds());
            RefreshResponse response = new RefreshResponse(
                    accessToken,
                    newRawToken,
                    "Bearer",
                    TokenService.ACCESS_TOKEN_VALIDITY_SECONDS,
                    remainingSeconds
            );

            return RefreshExecutionResult.success(response);
        });

        if (result == null || !result.isSuccess()) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        return result.getResponse();
    }

    public void logout(String rawRefreshToken, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawRefreshToken);
        transactionTemplate.executeWithoutResult(status -> {
            Optional<RefreshSessionEntity> sessionOpt = refreshSessionRepository.findByTokenHash(tokenHash);

            if (sessionOpt.isEmpty()) {
                return;
            }

            RefreshSessionEntity session = sessionOpt.get();
            Instant now = Instant.now();
            refreshSessionRepository.revokeFamily(session.getFamilyId(), now);

            Optional<UserEntity> userOpt = userRepository.findById(session.getUserId());
            String username = userOpt.map(UserEntity::getUsername).orElse("unknown");

            auditLogService.recordEventIndependent("LOGOUT", session.getUserId(), username, ipAddress,
                    "Session family " + session.getFamilyId() + " logged out");
        });
    }
}
