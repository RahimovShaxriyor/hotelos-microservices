package com.hotelos.identity.persistence.repository;

import com.hotelos.identity.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Modifying
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @Query(value = """
        UPDATE identity.users
        SET failed_login_attempts = failed_login_attempts + 1,
            locked_until = CASE
                WHEN failed_login_attempts + 1 >= 5 THEN NOW() + INTERVAL '15 minutes'
                ELSE locked_until
            END,
            updated_at = NOW()
        WHERE username = :username
    """, nativeQuery = true)
    int recordFailedLoginAttempt(@Param("username") String username);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = """
        UPDATE identity.users
        SET failed_login_attempts = 0,
            locked_until = NULL,
            updated_at = NOW()
        WHERE id = :userId
    """, nativeQuery = true)
    int resetFailedLoginAttempts(@Param("userId") UUID userId);
}
