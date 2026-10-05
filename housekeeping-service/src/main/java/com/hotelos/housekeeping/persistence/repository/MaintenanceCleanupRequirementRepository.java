package com.hotelos.housekeeping.persistence.repository;

import com.hotelos.housekeeping.persistence.entity.MaintenanceCleanupRequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceCleanupRequirementRepository extends JpaRepository<MaintenanceCleanupRequirementEntity, UUID> {

    List<MaintenanceCleanupRequirementEntity> findByRoomNumberAndSatisfiedAtIsNull(String roomNumber);

    boolean existsByRoomNumberAndSatisfiedAtIsNull(String roomNumber);

    @Query(value = "SELECT * FROM housekeeping.maintenance_cleanup_requirements WHERE room_number = :roomNumber AND satisfied_at IS NULL FOR UPDATE", nativeQuery = true)
    List<MaintenanceCleanupRequirementEntity> findPendingForRoomForUpdate(@Param("roomNumber") String roomNumber);
}
