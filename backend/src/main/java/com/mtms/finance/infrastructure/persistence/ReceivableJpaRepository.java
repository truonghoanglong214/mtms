package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.ReceivableStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceivableJpaRepository extends JpaRepository<ReceivableJpaEntity, UUID> {

    Optional<ReceivableJpaEntity> findByBranchIdAndId(UUID branchId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReceivableJpaEntity r WHERE r.id = :id")
    Optional<ReceivableJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReceivableJpaEntity r WHERE r.branchId = :branchId AND r.id = :id")
    Optional<ReceivableJpaEntity> findByBranchIdAndIdForUpdate(@Param("branchId") UUID branchId, @Param("id") UUID id);

    @Query("SELECT r FROM ReceivableJpaEntity r WHERE r.branchId = :branchId " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:search IS NULL OR LOWER(r.counterpartyLabel) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ReceivableJpaEntity> findByBranchIdWithFilters(
            @Param("branchId") UUID branchId,
            @Param("status") ReceivableStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(r.originalAmount - r.receivedAmount), 0) FROM ReceivableJpaEntity r " +
           "WHERE r.branchId = :branchId AND r.status = com.mtms.finance.domain.ReceivableStatus.OPEN")
    java.math.BigDecimal sumOutstandingReceivablesByBranchId(@Param("branchId") UUID branchId);
}
