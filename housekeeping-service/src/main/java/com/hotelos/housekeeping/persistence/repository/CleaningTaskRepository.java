package com.hotelos.housekeeping.persistence.repository;

import com.hotelos.housekeeping.persistence.entity.CleaningTaskEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CleaningTaskRepository extends JpaRepository<CleaningTaskEntity, UUID> {

    Optional<CleaningTaskEntity> findByTurnoverCorrelationId(String turnoverCorrelationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM CleaningTaskEntity t WHERE t.roomNumber = :roomNumber AND t.taskStatus IN (com.hotelos.housekeeping.domain.CleaningStatus.WAITING, com.hotelos.housekeeping.domain.CleaningStatus.CLEANING)")
    Optional<CleaningTaskEntity> findActiveTaskForUpdate(@Param("roomNumber") String roomNumber);

    @Query("SELECT t FROM CleaningTaskEntity t WHERE t.taskStatus IN (com.hotelos.housekeeping.domain.CleaningStatus.WAITING, com.hotelos.housekeeping.domain.CleaningStatus.CLEANING) ORDER BY t.createdAt ASC, t.id ASC")
    List<CleaningTaskEntity> findActiveQueue();
}
