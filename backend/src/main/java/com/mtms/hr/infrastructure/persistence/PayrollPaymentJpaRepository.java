package com.mtms.hr.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollPaymentJpaRepository extends JpaRepository<PayrollPaymentJpaEntity, UUID> {

    List<PayrollPaymentJpaEntity> findByPayrollLineIdOrderByPaidOnAscCreatedAtAsc(UUID payrollLineId);

    List<PayrollPaymentJpaEntity> findByPayrollLineId(UUID payrollLineId);

    List<PayrollPaymentJpaEntity> findByAccountId(UUID accountId);

    List<PayrollPaymentJpaEntity> findByPaidOn(LocalDate paidOn);

    void deleteByPayrollLineId(UUID payrollLineId);
}
