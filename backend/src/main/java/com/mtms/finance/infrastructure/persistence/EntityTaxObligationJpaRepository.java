package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.TaxObligationStatus;
import com.mtms.finance.domain.TaxObligationType;
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
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * Repository for entity and branch tax manual obligations.
 */
@Repository
public interface EntityTaxObligationJpaRepository extends JpaRepository<EntityTaxObligationJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM EntityTaxObligationJpaEntity t WHERE t.id = :id")
    Optional<EntityTaxObligationJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByBusinessEntityIdAndObligationCode(UUID businessEntityId, String obligationCode);

    @Query("SELECT t FROM EntityTaxObligationJpaEntity t " +
           "WHERE (t.branchId = :branchId OR (t.businessEntityId = :businessEntityId AND t.branchId IS NULL)) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:obligationType IS NULL OR t.obligationType = :obligationType) " +
           "AND (CAST(:startDate AS date) IS NULL OR t.taxPeriodStart >= :startDate) " +
           "AND (CAST(:endDate AS date) IS NULL OR t.taxPeriodEnd <= :endDate)")
    Page<EntityTaxObligationJpaEntity> findByBranchOrEntity(
            @Param("branchId") UUID branchId,
            @Param("businessEntityId") UUID businessEntityId,
            @Param("status") TaxObligationStatus status,
            @Param("obligationType") TaxObligationType obligationType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(t.amountDue - t.amountPaid), 0) FROM EntityTaxObligationJpaEntity t " +
           "WHERE t.branchId = :branchId AND t.status IN (com.mtms.finance.domain.TaxObligationStatus.OPEN, com.mtms.finance.domain.TaxObligationStatus.PARTIALLY_PAID)")
    java.math.BigDecimal sumOutstandingTaxByBranchId(@Param("branchId") UUID branchId);
}
