package com.hotelos.identity.service;

import com.hotelos.identity.persistence.entity.AuthAuditLogEntity;
import com.hotelos.identity.persistence.repository.AuthAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthAuditLogService {

    private final AuthAuditLogRepository repository;

    public AuthAuditLogService(AuthAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String eventType, UUID userId, String username, String ipAddress, String details) {
        AuthAuditLogEntity entity = new AuthAuditLogEntity(
                eventType,
                userId,
                username,
                ipAddress,
                details,
                Instant.now()
        );
        repository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEventIndependent(String eventType, UUID userId, String username, String ipAddress, String details) {
        AuthAuditLogEntity entity = new AuthAuditLogEntity(
                eventType,
                userId,
                username,
                ipAddress,
                details,
                Instant.now()
        );
        repository.save(entity);
    }
}
