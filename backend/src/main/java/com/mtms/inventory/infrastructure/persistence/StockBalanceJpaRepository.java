package com.mtms.inventory.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockBalanceJpaRepository extends JpaRepository<StockBalanceJpaEntity, UUID> {

    Optional<StockBalanceJpaEntity> findByBranchIdAndItemId(UUID branchId, UUID itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBalanceJpaEntity b WHERE b.branchId = :branchId AND b.itemId = :itemId")
    Optional<StockBalanceJpaEntity> findWithLockByBranchIdAndItemId(@Param("branchId") UUID branchId, @Param("itemId") UUID itemId);

    List<StockBalanceJpaEntity> findByBranchId(UUID branchId);

    Page<StockBalanceJpaEntity> findByBranchId(UUID branchId, Pageable pageable);

    @Query("SELECT b FROM StockBalanceJpaEntity b JOIN b.catalogItem i " +
           "WHERE b.branchId = :branchId " +
           "AND (:search IS NULL OR LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<StockBalanceJpaEntity> searchByBranchId(
            @Param("branchId") UUID branchId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(b.inventoryValue), 0) FROM StockBalanceJpaEntity b WHERE b.branchId = :branchId")
    BigDecimal sumInventoryValueByBranchId(@Param("branchId") UUID branchId);

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM StockBalanceJpaEntity b WHERE b.branchId = :branchId")
    BigDecimal sumQuantityByBranchId(@Param("branchId") UUID branchId);

    @Query("SELECT COUNT(b) FROM StockBalanceJpaEntity b WHERE b.branchId = :branchId")
    long countByBranchId(@Param("branchId") UUID branchId);

    @Query("SELECT COUNT(b) FROM StockBalanceJpaEntity b WHERE b.branchId = :branchId AND b.quantity <= :threshold")
    long countLowStockByBranchId(@Param("branchId") UUID branchId, @Param("threshold") BigDecimal threshold);

    @Query("SELECT b FROM StockBalanceJpaEntity b " +
           "LEFT JOIN FETCH b.catalogItem i " +
           "LEFT JOIN FETCH i.baseUnit u " +
           "WHERE b.branchId = :branchId " +
           "ORDER BY b.inventoryValue DESC")
    List<StockBalanceJpaEntity> findTopValuedByBranchId(@Param("branchId") UUID branchId, Pageable pageable);

    @Query("SELECT b FROM StockBalanceJpaEntity b " +
           "LEFT JOIN FETCH b.catalogItem i " +
           "LEFT JOIN FETCH i.baseUnit u " +
           "WHERE b.branchId = :branchId")
    List<StockBalanceJpaEntity> findByBranchIdWithCatalogItem(@Param("branchId") UUID branchId);
}
