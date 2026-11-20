package com.mtms.stockcount.infrastructure.persistence;

import com.mtms.stockcount.domain.model.StockCountStatus;
import com.mtms.stockcount.domain.model.StockCountType;
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
public interface SpringDataStockCountRepository extends JpaRepository<StockCountJpaEntity, UUID> {

    @Query("SELECT c FROM StockCountJpaEntity c LEFT JOIN FETCH c.lines WHERE c.id = :id")
    Optional<StockCountJpaEntity> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT c FROM StockCountJpaEntity c LEFT JOIN FETCH c.lines WHERE c.branchId = :branchId AND c.id = :id")
    Optional<StockCountJpaEntity> findByIdAndBranchWithLines(@Param("id") UUID id, @Param("branchId") UUID branchId);

    @Query("SELECT c FROM StockCountJpaEntity c WHERE c.branchId = :branchId " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:countType IS NULL OR c.countType = :countType) " +
           "AND (:startDate IS NULL OR c.countedOn >= :startDate) " +
           "AND (:endDate IS NULL OR c.countedOn <= :endDate) " +
           "AND (:search IS NULL OR LOWER(c.countNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(c.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<StockCountJpaEntity> findWithFilters(
            @Param("branchId") UUID branchId,
            @Param("status") StockCountStatus status,
            @Param("countType") StockCountType countType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(c) FROM StockCountJpaEntity c WHERE c.branchId = :branchId " +
           "AND EXTRACT(YEAR FROM c.countedOn) = :year AND EXTRACT(MONTH FROM c.countedOn) = :month")
    long countByBranchIdAndYearMonth(@Param("branchId") UUID branchId, @Param("year") int year, @Param("month") int month);
}
