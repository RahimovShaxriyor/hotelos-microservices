package com.hotelos.reception.persistence.repository;

import com.hotelos.reception.domain.FolioEntryType;
import com.hotelos.reception.domain.FolioSourceType;
import com.hotelos.reception.persistence.entity.FolioEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolioEntryRepository extends JpaRepository<FolioEntryEntity, UUID> {

    List<FolioEntryEntity> findByFolioIdOrderByOccurredAtAscCreatedAtAsc(UUID folioId);

    Page<FolioEntryEntity> findByFolioId(UUID folioId, Pageable pageable);

    Page<FolioEntryEntity> findByFolioIdAndEntryType(UUID folioId, FolioEntryType entryType, Pageable pageable);

    Optional<FolioEntryEntity> findByFolioIdAndSourceTypeAndSourceId(UUID folioId, FolioSourceType sourceType, String sourceId);

    Optional<FolioEntryEntity> findBySourceTypeAndSourceId(FolioSourceType sourceType, String sourceId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM FolioEntryEntity e WHERE e.folioId = :folioId")
    BigDecimal calculateBalanceByFolioId(@Param("folioId") UUID folioId);
}
