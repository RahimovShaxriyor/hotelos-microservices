package com.hotelos.identity.service;

import com.hotelos.identity.domain.UserStatus;
import com.hotelos.identity.persistence.entity.UserEntity;
import com.hotelos.identity.persistence.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);
    private static final long BOOTSTRAP_ADVISORY_LOCK_ID = 8086001L;

    private final UserRepository userRepository;
    private final AuthAuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public AdminBootstrapService(
            UserRepository userRepository,
            AuthAuditLogService auditLogService,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Transactional
    public void executeBootstrap(String bootstrapUsername, String bootstrapPassword) {
        // Concurrency protection: acquire PostgreSQL advisory transaction lock across potential replicas
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", BOOTSTRAP_ADVISORY_LOCK_ID)
                .getSingleResult();

        if (bootstrapUsername == null || bootstrapUsername.isBlank() ||
                bootstrapPassword == null || bootstrapPassword.isBlank()) {
            log.info("Bootstrap credentials not configured; bootstrap skipped.");
            return;
        }

        if (userRepository.count() > 0) {
            log.info("Users already exist in database; bootstrap skipped.");
            return;
        }

        String normalizedUsername = bootstrapUsername.trim().toLowerCase(Locale.ROOT);
        String passwordHash = passwordEncoder.encode(bootstrapPassword);

        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        UserEntity admin = new UserEntity(
                userId,
                normalizedUsername,
                passwordHash,
                "System Administrator",
                UserStatus.ACTIVE,
                now
        );
        admin.addRole("ADMIN");
        userRepository.save(admin);

        auditLogService.recordEvent(
                "ADMIN_BOOTSTRAPPED",
                userId,
                normalizedUsername,
                "127.0.0.1",
                "Initial administrator bootstrapped on system initialization"
        );

        log.info("Initial administrator account successfully provisioned for username: [{}]", normalizedUsername);
    }
}
