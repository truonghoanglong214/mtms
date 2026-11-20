package com.mtms.hr.infrastructure.persistence;

import com.mtms.hr.domain.PayrollPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollPeriodJpaRepository extends JpaRepository<PayrollPeriodJpaEntity, UUID> {

    Optional<PayrollPeriodJpaEntity> findByBranchIdAndMonthStartAndIsCurrentTrue(UUID branchId, LocalDate monthStart);

    Optional<PayrollPeriodJpaEntity> findByIdAndBranchId(UUID id, UUID branchId);

    Optional<PayrollPeriodJpaEntity> findTopByBranchIdAndMonthStartOrderByRevisionDesc(UUID branchId, LocalDate monthStart);

    @Query("""
        SELECT p FROM PayrollPeriodJpaEntity p
        WHERE p.branchId = :branchId
          AND (:year IS NULL OR EXTRACT(YEAR FROM p.monthStart) = :year)
          AND (:status IS NULL OR p.status = :status)
        ORDER BY p.monthStart DESC, p.revision DESC
    """)
    List<PayrollPeriodJpaEntity> findFiltered(
            @Param("branchId") UUID branchId,
            @Param("year") Integer year,
            @Param("status") PayrollPeriodStatus status
    );
}
