package com.hotelos.identity.persistence.repository;

import com.hotelos.identity.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query(value = "SELECT pg_advisory_xact_lock(8086002)", nativeQuery = true)
    void acquireGovernanceAdvisoryLock();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.username = :username")
    Optional<UserEntity> findByUsernameForUpdate(@Param("username") String username);

    @Query(value = """
        SELECT COUNT(DISTINCT u.id)
        FROM identity.users u
        JOIN identity.user_roles ur ON u.id = ur.user_id
        WHERE ur.role_name = :roleName
          AND u.status = 'ACTIVE'
          AND u.id != :excludedUserId
    """, nativeQuery = true)
    long countActiveUsersWithRoleExcluding(@Param("roleName") String roleName, @Param("excludedUserId") UUID excludedUserId);

    @Query(value = """
        SELECT COUNT(DISTINCT u.id)
        FROM identity.users u
        JOIN identity.user_roles ur ON u.id = ur.user_id
        WHERE ur.role_name = :roleName
          AND u.status = 'ACTIVE'
    """, nativeQuery = true)
    long countActiveUsersWithRole(@Param("roleName") String roleName);

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
