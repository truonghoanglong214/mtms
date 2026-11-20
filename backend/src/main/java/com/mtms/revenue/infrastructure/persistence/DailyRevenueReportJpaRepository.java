package com.mtms.revenue.infrastructure.persistence;

import com.mtms.revenue.domain.RevenueReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyRevenueReportJpaRepository extends JpaRepository<DailyRevenueReportJpaEntity, UUID> {

    Optional<DailyRevenueReportJpaEntity> findByBranchIdAndBusinessDateAndIsCurrentTrue(
            UUID branchId,
            LocalDate businessDate
    );

    Optional<DailyRevenueReportJpaEntity> findByIdAndBranchId(UUID id, UUID branchId);

    Optional<DailyRevenueReportJpaEntity> findByBranchIdAndBusinessDateAndRevision(
            UUID branchId,
            LocalDate businessDate,
            int revision
    );

    List<DailyRevenueReportJpaEntity> findByBranchIdAndBusinessDateInAndIsCurrentTrue(
            UUID branchId,
            Collection<LocalDate> businessDates
    );

    List<DailyRevenueReportJpaEntity> findByBranchIdAndBusinessDateOrderByRevisionDesc(
            UUID branchId,
            LocalDate businessDate
    );

    @Query("SELECT r FROM DailyRevenueReportJpaEntity r WHERE r.branchId = :branchId " +
           "AND (:startDate IS NULL OR r.businessDate >= :startDate) " +
           "AND (:endDate IS NULL OR r.businessDate <= :endDate) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND r.isCurrent = true " +
           "ORDER BY r.businessDate DESC")
    Page<DailyRevenueReportJpaEntity> findCurrentReports(
            @Param("branchId") UUID branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") RevenueReportStatus status,
            Pageable pageable
    );
}
