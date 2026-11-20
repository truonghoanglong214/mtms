package com.mtms.movement.infrastructure.persistence;

import com.mtms.movement.domain.model.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataStockTransferRepository extends JpaRepository<StockTransferJpaEntity, UUID> {

    @Query("SELECT t FROM StockTransferJpaEntity t LEFT JOIN FETCH t.lines WHERE t.id = :id")
    Optional<StockTransferJpaEntity> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT t FROM StockTransferJpaEntity t LEFT JOIN FETCH t.lines " +
           "WHERE (t.fromBranchId = :branchId OR t.toBranchId = :branchId) AND t.id = :id")
    Optional<StockTransferJpaEntity> findByIdAndBranchWithLines(@Param("id") UUID id, @Param("branchId") UUID branchId);

    @Query("SELECT t FROM StockTransferJpaEntity t WHERE t.fromBranchId = :fromBranchId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:startDate IS NULL OR t.requestedOn >= :startDate) " +
           "AND (:endDate IS NULL OR t.requestedOn <= :endDate) " +
           "AND (:search IS NULL OR LOWER(t.transferNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(t.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<StockTransferJpaEntity> findOutboundWithFilters(
            @Param("fromBranchId") UUID fromBranchId,
            @Param("status") TransferStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT t FROM StockTransferJpaEntity t WHERE t.toBranchId = :toBranchId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:startDate IS NULL OR t.requestedOn >= :startDate) " +
           "AND (:endDate IS NULL OR t.requestedOn <= :endDate) " +
           "AND (:search IS NULL OR LOWER(t.transferNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(t.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<StockTransferJpaEntity> findInboundWithFilters(
            @Param("toBranchId") UUID toBranchId,
            @Param("status") TransferStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM StockTransferJpaEntity t WHERE t.businessEntityId = :entityId " +
           "AND t.requestedOn >= :startDate AND t.requestedOn <= :endDate")
    long countByBusinessEntityIdAndRequestedOnBetween(
            @Param("entityId") UUID entityId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
