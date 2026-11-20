package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.ReconciliationDisposition;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * Repository for cash and bank account reconciliation snapshots.
 */
@Repository
public interface CashReconciliationJpaRepository extends JpaRepository<CashReconciliationJpaEntity, UUID> {

    Optional<CashReconciliationJpaEntity> findByBranchIdAndId(UUID branchId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CashReconciliationJpaEntity r WHERE r.id = :id AND r.branchId = :branchId")
    Optional<CashReconciliationJpaEntity> findByIdAndBranchIdForUpdate(@Param("branchId") UUID branchId, @Param("id") UUID id);

    boolean existsByAccountIdAndReconciliationDate(UUID accountId, LocalDate reconciliationDate);

    Optional<CashReconciliationJpaEntity> findByAccountIdAndReconciliationDate(UUID accountId, LocalDate reconciliationDate);

    @Query("SELECT r FROM CashReconciliationJpaEntity r WHERE r.branchId = :branchId " +
           "AND (:accountId IS NULL OR r.accountId = :accountId) " +
           "AND (:startDate IS NULL OR r.reconciliationDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.reconciliationDate <= :endDate) " +
           "AND (:disposition IS NULL OR r.disposition = :disposition) " +
           "ORDER BY r.reconciliationDate DESC, r.createdAt DESC")
    Page<CashReconciliationJpaEntity> findReconciliations(
            @Param("branchId") UUID branchId,
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("disposition") ReconciliationDisposition disposition,
            Pageable pageable
    );
}
