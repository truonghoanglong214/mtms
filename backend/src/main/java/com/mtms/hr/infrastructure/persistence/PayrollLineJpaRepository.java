package com.mtms.hr.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollLineJpaRepository extends JpaRepository<PayrollLineJpaEntity, UUID> {

    List<PayrollLineJpaEntity> findByPayrollPeriodId(UUID payrollPeriodId);

    Optional<PayrollLineJpaEntity> findByPayrollPeriodIdAndEmployeeId(UUID payrollPeriodId, UUID employeeId);

    Optional<PayrollLineJpaEntity> findByIdAndPayrollPeriodId(UUID id, UUID payrollPeriodId);

    List<PayrollLineJpaEntity> findByEmployeeId(UUID employeeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM PayrollLineJpaEntity l WHERE l.id = :id")
    Optional<PayrollLineJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    void deleteByPayrollPeriodId(UUID payrollPeriodId);
}
