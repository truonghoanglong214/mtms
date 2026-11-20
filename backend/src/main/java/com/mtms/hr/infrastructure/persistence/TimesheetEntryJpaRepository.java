package com.mtms.hr.infrastructure.persistence;

import com.mtms.hr.domain.TimesheetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetEntryJpaRepository extends JpaRepository<TimesheetEntryJpaEntity, UUID> {

    Optional<TimesheetEntryJpaEntity> findByEmployeeIdAndBranchIdAndWorkDate(
            UUID employeeId,
            UUID branchId,
            LocalDate workDate
    );

    Optional<TimesheetEntryJpaEntity> findByIdAndBranchId(UUID id, UUID branchId);

    List<TimesheetEntryJpaEntity> findByIdInAndBranchId(List<UUID> ids, UUID branchId);

    List<TimesheetEntryJpaEntity> findByBranchIdAndWorkDateBetween(
            UUID branchId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("""
        SELECT t FROM TimesheetEntryJpaEntity t
        WHERE t.employeeId = :employeeId
          AND t.workDate >= :startDate
          AND t.workDate <= :endDate
          AND t.status = :status
        ORDER BY t.workDate ASC
    """)
    List<TimesheetEntryJpaEntity> findByEmployeeIdAndDateRangeAndStatus(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") TimesheetStatus status
    );

    @Query("""
        SELECT t FROM TimesheetEntryJpaEntity t
        WHERE t.branchId = :branchId
          AND (:startDate IS NULL OR t.workDate >= :startDate)
          AND (:endDate IS NULL OR t.workDate <= :endDate)
          AND (:employeeId IS NULL OR t.employeeId = :employeeId)
          AND (:status IS NULL OR t.status = :status)
        ORDER BY t.workDate DESC, t.createdAt DESC
    """)
    Page<TimesheetEntryJpaEntity> findFiltered(
            @Param("branchId") UUID branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("employeeId") UUID employeeId,
            @Param("status") TimesheetStatus status,
            Pageable pageable
    );
}
