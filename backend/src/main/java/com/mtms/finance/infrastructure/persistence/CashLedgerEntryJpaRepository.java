package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashLedgerEntryJpaRepository extends JpaRepository<CashLedgerEntryJpaEntity, UUID> {

    boolean existsByAccountIdAndSourceTypeAndSourceIdAndSourceLeg(
            UUID accountId,
            SourceType sourceType,
            UUID sourceId,
            String sourceLeg
    );

    Optional<CashLedgerEntryJpaEntity> findByAccountIdAndSourceTypeAndSourceIdAndSourceLeg(
            UUID accountId,
            SourceType sourceType,
            UUID sourceId,
            String sourceLeg
    );

    List<CashLedgerEntryJpaEntity> findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(
            UUID branchId,
            SourceType sourceType,
            UUID sourceId
    );

    List<CashLedgerEntryJpaEntity> findBySourceTypeAndSourceIdOrderByPostedAtAsc(
            SourceType sourceType,
            UUID sourceId
    );

    @Query("SELECT e FROM CashLedgerEntryJpaEntity e WHERE e.branchId = :branchId " +
           "AND (:accountId IS NULL OR e.accountId = :accountId) " +
           "AND (:categoryId IS NULL OR e.categoryId = :categoryId) " +
           "AND (:sourceType IS NULL OR e.sourceType = :sourceType) " +
           "AND (:startDate IS NULL OR e.businessDate >= :startDate) " +
           "AND (:endDate IS NULL OR e.businessDate <= :endDate) " +
           "ORDER BY e.postedAt DESC, e.businessDate DESC")
    Page<CashLedgerEntryJpaEntity> findEntries(
            @Param("branchId") UUID branchId,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("sourceType") SourceType sourceType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    List<CashLedgerEntryJpaEntity> findByBranchIdOrderByPostedAtDesc(UUID branchId);
}
