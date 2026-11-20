package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.OpeningBatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009
 * Repository for opening balance batches.
 */
@Repository
public interface OpeningBalanceBatchJpaRepository extends JpaRepository<OpeningBalanceBatchJpaEntity, UUID> {

    Optional<OpeningBalanceBatchJpaEntity> findByBranchId(UUID branchId);

    Optional<OpeningBalanceBatchJpaEntity> findByIdAndBranchId(UUID id, UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM OpeningBalanceBatchJpaEntity b WHERE b.id = :id")
    Optional<OpeningBalanceBatchJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByBranchIdAndStatus(UUID branchId, OpeningBatchStatus status);

    boolean existsByBranchId(UUID branchId);
}
