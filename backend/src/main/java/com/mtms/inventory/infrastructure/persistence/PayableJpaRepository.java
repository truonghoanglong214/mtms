package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.PayableStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PayableJpaRepository extends JpaRepository<PayableJpaEntity, UUID> {

    Optional<PayableJpaEntity> findByBranchIdAndId(UUID branchId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PayableJpaEntity p WHERE p.id = :id")
    Optional<PayableJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PayableJpaEntity p WHERE p.branchId = :branchId AND p.id = :id")
    Optional<PayableJpaEntity> findByBranchIdAndIdForUpdate(@Param("branchId") UUID branchId, @Param("id") UUID id);

    @Query("SELECT p FROM PayableJpaEntity p WHERE p.branchId = :branchId " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:search IS NULL OR LOWER(p.counterpartyLabel) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PayableJpaEntity> findByBranchIdWithFilters(
            @Param("branchId") UUID branchId,
            @Param("status") PayableStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(p.originalAmount - p.paidAmount), 0) FROM PayableJpaEntity p " +
           "WHERE p.branchId = :branchId AND p.status IN ('OPEN', 'DISPUTED')")
    BigDecimal sumOutstandingDebtByBranchId(@Param("branchId") UUID branchId);
}
