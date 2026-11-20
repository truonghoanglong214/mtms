package com.mtms.hr.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EmployeeAssignmentJpaRepository extends JpaRepository<EmployeeAssignmentJpaEntity, UUID> {

    List<EmployeeAssignmentJpaEntity> findByEmployeeIdOrderByEffectiveFromAscCreatedAtAsc(UUID employeeId);

    List<EmployeeAssignmentJpaEntity> findByEmployeeIdAndBranchId(UUID employeeId, UUID branchId);

    boolean existsByEmployeeIdAndBranchId(UUID employeeId, UUID branchId);

    @Query("""
        SELECT a FROM EmployeeAssignmentJpaEntity a
        WHERE a.employeeId = :employeeId
          AND a.isPrimary = true
          AND a.effectiveFrom <= :date
          AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        ORDER BY a.effectiveFrom DESC
    """)
    List<EmployeeAssignmentJpaEntity> findActivePrimaryAssignmentsOnDate(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT a FROM EmployeeAssignmentJpaEntity a
        WHERE a.employeeId = :employeeId
          AND a.isPrimary = true
          AND (a.effectiveTo IS NULL OR a.effectiveTo >= :effectiveFrom)
        ORDER BY a.effectiveFrom ASC
    """)
    List<EmployeeAssignmentJpaEntity> findConflictingPrimaryAssignments(
            @Param("employeeId") UUID employeeId,
            @Param("effectiveFrom") LocalDate effectiveFrom
    );

    @Query("""
        SELECT a FROM EmployeeAssignmentJpaEntity a
        WHERE a.branchId = :branchId
          AND a.isPrimary = true
          AND a.effectiveFrom <= :date
          AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        ORDER BY a.effectiveFrom ASC
    """)
    List<EmployeeAssignmentJpaEntity> findActivePrimaryAssignmentsByBranchOnDate(
            @Param("branchId") UUID branchId,
            @Param("date") LocalDate date
    );
}
