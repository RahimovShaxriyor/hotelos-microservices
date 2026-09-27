package com.hotelos.maintenance.persistence.repository;

import com.hotelos.maintenance.persistence.entity.MaintenanceIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceIssueRepository extends JpaRepository<MaintenanceIssueEntity, UUID> {

    @Query(value = "SELECT * FROM maintenance.maintenance_issues WHERE status = 'OPEN' ORDER BY priority_rank ASC, created_at ASC, id ASC FOR UPDATE SKIP LOCKED LIMIT 1", nativeQuery = true)
    Optional<MaintenanceIssueEntity> claimNextOpenIssue();

    @Query(value = "SELECT * FROM maintenance.maintenance_issues WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<MaintenanceIssueEntity> lockIssue(@Param("id") UUID id);

    @Query("SELECT i FROM MaintenanceIssueEntity i WHERE i.status = com.hotelos.maintenance.domain.IssueStatus.OPEN ORDER BY i.priorityRank ASC, i.createdAt ASC, i.id ASC")
    List<MaintenanceIssueEntity> findOpenQueue();

    @Query("SELECT COUNT(i) FROM MaintenanceIssueEntity i WHERE i.roomNumber = :roomNumber AND i.status IN (com.hotelos.maintenance.domain.IssueStatus.OPEN, com.hotelos.maintenance.domain.IssueStatus.ASSIGNED)")
    long countActiveIssuesForRoom(@Param("roomNumber") String roomNumber);

    @Query("SELECT i FROM MaintenanceIssueEntity i ORDER BY i.createdAt ASC")
    List<MaintenanceIssueEntity> findAllOrderedByCreatedAt();
}
