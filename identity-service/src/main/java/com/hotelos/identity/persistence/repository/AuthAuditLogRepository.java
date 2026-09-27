package com.hotelos.identity.persistence.repository;

import com.hotelos.identity.persistence.entity.AuthAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLogEntity, Long> {
    List<AuthAuditLogEntity> findByUsernameOrderByCreatedAtDesc(String username);
}
