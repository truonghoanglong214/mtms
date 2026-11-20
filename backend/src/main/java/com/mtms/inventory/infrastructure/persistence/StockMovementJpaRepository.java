package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementJpaRepository extends JpaRepository<StockMovementJpaEntity, UUID> {

    List<StockMovementJpaEntity> findByBranchId(UUID branchId);

    @Query("SELECT m FROM StockMovementJpaEntity m " +
           "LEFT JOIN m.catalogItem i " +
           "WHERE m.branchId = :branchId " +
           "AND (:movementType IS NULL OR m.movementType = :movementType) " +
           "AND (:itemId IS NULL OR m.itemId = :itemId) " +
           "AND (:startDate IS NULL OR m.businessDate >= :startDate) " +
           "AND (:endDate IS NULL OR m.businessDate <= :endDate) " +
           "AND (:search IS NULL OR LOWER(m.sourceRef) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR (i IS NOT NULL AND (LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))))")
    Page<StockMovementJpaEntity> findByBranchIdWithFilters(
            @Param("branchId") UUID branchId,
            @Param("movementType") StockMovementType movementType,
            @Param("itemId") UUID itemId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(m.signedValue), 0) FROM StockMovementJpaEntity m " +
           "WHERE m.branchId = :branchId " +
           "AND m.movementType = :movementType " +
           "AND m.businessDate >= :startDate AND m.businessDate <= :endDate")
    BigDecimal sumSignedValueByBranchIdAndMovementTypeAndDateBetween(
            @Param("branchId") UUID branchId,
            @Param("movementType") StockMovementType movementType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT m FROM StockMovementJpaEntity m " +
           "LEFT JOIN FETCH m.catalogItem i " +
           "LEFT JOIN FETCH i.baseUnit u " +
           "WHERE m.branchId = :branchId " +
           "AND m.businessDate <= :endDate " +
           "ORDER BY m.businessDate ASC, m.createdAt ASC")
    List<StockMovementJpaEntity> findByBranchIdAndBusinessDateLessThanEqual(
            @Param("branchId") UUID branchId,
            @Param("endDate") LocalDate endDate
    );
}
