package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockDocumentStatus;
import com.mtms.inventory.domain.StockDocumentType;
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
public interface StockDocumentJpaRepository extends JpaRepository<StockDocumentJpaEntity, UUID> {

    @Query("SELECT d FROM StockDocumentJpaEntity d LEFT JOIN FETCH d.lines WHERE d.branchId = :branchId AND d.id = :id")
    Optional<StockDocumentJpaEntity> findByBranchIdAndIdWithLines(@Param("branchId") UUID branchId, @Param("id") UUID id);

    @Query("SELECT d FROM StockDocumentJpaEntity d LEFT JOIN FETCH d.lines WHERE d.id = :id")
    Optional<StockDocumentJpaEntity> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT d FROM StockDocumentJpaEntity d WHERE d.branchId = :branchId " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:documentType IS NULL OR d.documentType = :documentType) " +
           "AND (:startDate IS NULL OR d.documentDate >= :startDate) " +
           "AND (:endDate IS NULL OR d.documentDate <= :endDate) " +
           "AND (:search IS NULL OR LOWER(d.documentNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(d.reason) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<StockDocumentJpaEntity> findByBranchIdWithFilters(
            @Param("branchId") UUID branchId,
            @Param("status") StockDocumentStatus status,
            @Param("documentType") StockDocumentType documentType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(d) FROM StockDocumentJpaEntity d WHERE d.branchId = :branchId " +
           "AND d.documentDate >= :startDate AND d.documentDate <= :endDate")
    long countByBranchIdAndDocumentDateBetween(
            @Param("branchId") UUID branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
