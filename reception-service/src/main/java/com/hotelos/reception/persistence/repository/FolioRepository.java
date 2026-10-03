package com.hotelos.reception.persistence.repository;

import com.hotelos.reception.persistence.entity.FolioEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolioRepository extends JpaRepository<FolioEntity, UUID> {

    Optional<FolioEntity> findByStayId(UUID stayId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FolioEntity f WHERE f.id = :id")
    Optional<FolioEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FolioEntity f WHERE f.stayId = :stayId")
    Optional<FolioEntity> findByStayIdForUpdate(@Param("stayId") UUID stayId);
}
