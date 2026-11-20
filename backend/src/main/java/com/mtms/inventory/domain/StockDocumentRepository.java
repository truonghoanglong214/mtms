package com.mtms.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-03
 * Domain repository interface for stock issue documents.
 */
public interface StockDocumentRepository {

    Optional<StockDocument> findById(UUID id);

    Optional<StockDocument> findByBranchIdAndId(UUID branchId, UUID id);

    Page<StockDocument> findByBranchIdWithFilters(
            UUID branchId,
            StockDocumentStatus status,
            StockDocumentType documentType,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    );

    long countByBranchIdAndMonth(UUID branchId, int year, int month);

    StockDocument save(StockDocument document);
}
