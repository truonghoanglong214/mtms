package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.ReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseReceiptJpaRepository extends JpaRepository<PurchaseReceiptJpaEntity, UUID> {

    @Query("SELECT r FROM PurchaseReceiptJpaEntity r LEFT JOIN FETCH r.lines WHERE r.branchId = :branchId AND r.id = :id")
    Optional<PurchaseReceiptJpaEntity> findByBranchIdAndIdWithLines(@Param("branchId") UUID branchId, @Param("id") UUID id);

    @Query("SELECT r FROM PurchaseReceiptJpaEntity r LEFT JOIN FETCH r.lines WHERE r.id = :id")
    Optional<PurchaseReceiptJpaEntity> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT r FROM PurchaseReceiptJpaEntity r WHERE r.branchId = :branchId " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:supplierId IS NULL OR r.supplierId = :supplierId) " +
           "AND (:startDate IS NULL OR r.receivedOn >= :startDate) " +
           "AND (:endDate IS NULL OR r.receivedOn <= :endDate) " +
           "AND (:search IS NULL OR LOWER(r.receiptNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(r.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PurchaseReceiptJpaEntity> findByBranchIdWithFilters(
            @Param("branchId") UUID branchId,
            @Param("status") ReceiptStatus status,
            @Param("supplierId") UUID supplierId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(r) FROM PurchaseReceiptJpaEntity r WHERE r.branchId = :branchId " +
           "AND r.receivedOn >= :startDate AND r.receivedOn <= :endDate")
    long countByBranchIdAndReceivedOnBetween(
            @Param("branchId") UUID branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
