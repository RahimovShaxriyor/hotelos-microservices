package com.hotelos.maintenance.persistence.repository;

import com.hotelos.maintenance.persistence.entity.TechnicianEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianRepository extends JpaRepository<TechnicianEntity, String> {

    @Query(value = "SELECT * FROM maintenance.technicians WHERE state = 'AVAILABLE' ORDER BY availability_sequence ASC FOR UPDATE SKIP LOCKED LIMIT 1", nativeQuery = true)
    Optional<TechnicianEntity> claimNextAvailableTechnician();

    @Query(value = "SELECT nextval('maintenance.technician_availability_seq')", nativeQuery = true)
    Long getNextAvailabilitySequence();

    @Modifying
    @Query("UPDATE TechnicianEntity t SET t.state = 'AVAILABLE', t.availabilitySequence = :seq, t.updatedAt = :now WHERE t.name = :name")
    void releaseTechnician(@Param("name") String name, @Param("seq") Long seq, @Param("now") Instant now);

    @Query("SELECT t FROM TechnicianEntity t ORDER BY t.name ASC")
    List<TechnicianEntity> findAllOrdered();

    @Modifying
    @Query(value = "UPDATE maintenance.technicians SET state = 'AVAILABLE', availability_sequence = 1, updated_at = :now WHERE name = 'Tech-1'", nativeQuery = true)
    void resetTech1(@Param("now") Instant now);

    @Modifying
    @Query(value = "UPDATE maintenance.technicians SET state = 'AVAILABLE', availability_sequence = 2, updated_at = :now WHERE name = 'Tech-2'", nativeQuery = true)
    void resetTech2(@Param("now") Instant now);

    @Query(value = "SELECT setval('maintenance.technician_availability_seq', 2)", nativeQuery = true)
    Long resetSequence();
}
