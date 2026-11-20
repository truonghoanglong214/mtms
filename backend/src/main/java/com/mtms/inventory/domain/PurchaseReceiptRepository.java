package com.mtms.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseReceiptRepository {

    Optional<PurchaseReceipt> findById(UUID id);

    Optional<PurchaseReceipt> findByBranchIdAndId(UUID branchId, UUID id);

    Page<PurchaseReceipt> findByBranchIdWithFilters(
            UUID branchId,
            ReceiptStatus status,
            UUID supplierId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    );

    long countByBranchIdAndMonth(UUID branchId, int year, int month);

    PurchaseReceipt save(PurchaseReceipt receipt);
}
