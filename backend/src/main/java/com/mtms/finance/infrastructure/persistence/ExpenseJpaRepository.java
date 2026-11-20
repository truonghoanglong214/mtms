package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.ExpenseStatus;
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

@Repository
public interface ExpenseJpaRepository extends JpaRepository<ExpenseJpaEntity, UUID> {

    Optional<ExpenseJpaEntity> findByBranchIdAndId(UUID branchId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ExpenseJpaEntity e WHERE e.id = :id AND e.branchId = :branchId")
    Optional<ExpenseJpaEntity> findByIdAndBranchIdForUpdate(@Param("branchId") UUID branchId, @Param("id") UUID id);

    boolean existsByBranchIdAndExpenseCode(UUID branchId, String expenseCode);

    long countByBranchIdAndExpenseDateBetween(UUID branchId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM ExpenseJpaEntity e WHERE e.branchId = :branchId " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:categoryId IS NULL OR e.categoryId = :categoryId) " +
           "AND (:startDate IS NULL OR e.expenseDate >= :startDate) " +
           "AND (:endDate IS NULL OR e.expenseDate <= :endDate) " +
           "AND (:reviewFlagOnly IS NULL OR :reviewFlagOnly = false OR e.reviewFlag = true) " +
           "AND (:search IS NULL OR LOWER(e.reason) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.expenseCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<ExpenseJpaEntity> findExpenses(
            @Param("branchId") UUID branchId,
            @Param("status") ExpenseStatus status,
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("reviewFlagOnly") Boolean reviewFlagOnly,
            @Param("search") String search,
            Pageable pageable
    );
}
