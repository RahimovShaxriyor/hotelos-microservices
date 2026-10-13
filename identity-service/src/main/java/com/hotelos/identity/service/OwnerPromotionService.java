package com.hotelos.identity.service;

import com.hotelos.identity.domain.AccountType;
import com.hotelos.identity.domain.UserStatus;
import com.hotelos.identity.persistence.entity.UserEntity;
import com.hotelos.identity.persistence.repository.RefreshSessionRepository;
import com.hotelos.identity.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class OwnerPromotionService {

    private static final Logger log = LoggerFactory.getLogger(OwnerPromotionService.class);

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final AuthAuditLogService auditLogService;

    public OwnerPromotionService(
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            AuthAuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void promoteOwnerIfConfigured(String rawUsername) {
        if (rawUsername == null || rawUsername.isBlank()) {
            return;
        }

        String normalizedUsername = rawUsername.trim().toLowerCase(Locale.ROOT);
        log.info("Evaluating operator owner promotion for username: [{}]", normalizedUsername);

        // Acquire governance advisory lock 8086002
        userRepository.acquireGovernanceAdvisoryLock();

        Optional<UserEntity> userOpt = userRepository.findByUsernameForUpdate(normalizedUsername);
        if (userOpt.isEmpty()) {
            log.warn("Operator owner promotion skipped: user [{}] does not exist", normalizedUsername);
            return;
        }

        UserEntity user = userOpt.get();
        if (user.getAccountType() != AccountType.STAFF) {
            log.warn("Operator owner promotion skipped: user [{}] is not STAFF", normalizedUsername);
            return;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Operator owner promotion skipped: user [{}] is not ACTIVE", normalizedUsername);
            return;
        }

        if (user.getRoles().contains("OWNER")) {
            log.info("User [{}] is already OWNER; operator promotion is idempotent.", normalizedUsername);
            return;
        }

        user.addRole("OWNER");
        user.setUpdatedAt(Instant.now());
        userRepository.saveAndFlush(user);

        // Revoke all refresh sessions
        refreshSessionRepository.revokeAllForUser(user.getId(), Instant.now());

        // Record governance audit event
        auditLogService.recordGovernanceEvent(
                "OWNER_PROMOTED_OPERATOR",
                null,
                user.getId(),
                normalizedUsername,
                null,
                normalizedUsername,
                "127.0.0.1",
                "Promoted to OWNER via environment operator runner"
        );

        log.info("User [{}] successfully promoted to OWNER by operator runner.", normalizedUsername);
    }
}
