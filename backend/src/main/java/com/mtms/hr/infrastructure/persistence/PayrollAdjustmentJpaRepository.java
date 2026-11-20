package com.mtms.hr.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollAdjustmentJpaRepository extends JpaRepository<PayrollAdjustmentJpaEntity, UUID> {

    List<PayrollAdjustmentJpaEntity> findByPayrollLineId(UUID payrollLineId);

    List<PayrollAdjustmentJpaEntity> findByPayrollLineIdOrderByCreatedAtAsc(UUID payrollLineId);

    List<PayrollAdjustmentJpaEntity> findByPayrollLineIdIn(List<UUID> payrollLineIds);

    void deleteByPayrollLineId(UUID payrollLineId);
}
