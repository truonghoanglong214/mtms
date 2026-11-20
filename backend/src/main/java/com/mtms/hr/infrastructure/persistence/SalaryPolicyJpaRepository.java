package com.mtms.hr.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalaryPolicyJpaRepository extends JpaRepository<SalaryPolicyJpaEntity, UUID> {

    List<SalaryPolicyJpaEntity> findByEmployeeIdOrderByEffectiveFromAscCreatedAtAsc(UUID employeeId);

    List<SalaryPolicyJpaEntity> findByEmployeeIdAndBranchIdOrderByEffectiveFromAscCreatedAtAsc(UUID employeeId, UUID branchId);

    @Query("""
        SELECT p FROM SalaryPolicyJpaEntity p
        WHERE p.employeeId = :employeeId
          AND p.branchId = :branchId
          AND p.effectiveFrom <= :date
          AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date)
        ORDER BY p.effectiveFrom DESC, p.createdAt DESC
    """)
    List<SalaryPolicyJpaEntity> findEffectivePolicies(
            @Param("employeeId") UUID employeeId,
            @Param("branchId") UUID branchId,
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT p FROM SalaryPolicyJpaEntity p
        WHERE p.employeeId = :employeeId
          AND p.branchId = :branchId
          AND (p.effectiveTo IS NULL OR p.effectiveTo >= :effectiveFrom)
        ORDER BY p.effectiveFrom DESC
    """)
    List<SalaryPolicyJpaEntity> findPoliciesToClose(
            @Param("employeeId") UUID employeeId,
            @Param("branchId") UUID branchId,
            @Param("effectiveFrom") LocalDate effectiveFrom
    );

    @Query("""
        SELECT p FROM SalaryPolicyJpaEntity p
        WHERE p.employeeId = :employeeId
          AND p.effectiveFrom <= :date
          AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date)
        ORDER BY p.effectiveFrom DESC, p.createdAt DESC
    """)
    List<SalaryPolicyJpaEntity> findEffectivePoliciesForEmployee(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date
    );
}
