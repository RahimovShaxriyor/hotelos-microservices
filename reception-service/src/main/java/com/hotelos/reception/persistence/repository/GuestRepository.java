package com.hotelos.reception.persistence.repository;

import com.hotelos.reception.persistence.entity.GuestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GuestRepository extends JpaRepository<GuestEntity, UUID> {

    Optional<GuestEntity> findByEmailIgnoreCase(String email);

    @Query("SELECT g FROM GuestEntity g WHERE " +
           "LOWER(g.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(g.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(g.email IS NOT NULL AND LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(g.phone IS NOT NULL AND g.phone LIKE CONCAT('%', :query, '%'))")
    Page<GuestEntity> searchGuests(@Param("query") String query, Pageable pageable);
}
