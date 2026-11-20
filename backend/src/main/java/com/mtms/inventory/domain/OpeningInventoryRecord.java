package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-INV-02 / BR-INV-08 / TC-024
 * Domain entity recording the single, immutable opening inventory event per branch.
 */
public class OpeningInventoryRecord {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final UUID importBatchId;
    private final LocalDate openingDate;
    private final int totalItems;
    private final BigDecimal totalQuantity;
    private final BigDecimal totalValue;
    private final String status;
    private final UUID approvedBy;
    private final Instant approvedAt;
    private final String note;

    public OpeningInventoryRecord(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            UUID importBatchId,
            LocalDate openingDate,
            int totalItems,
            BigDecimal totalQuantity,
            BigDecimal totalValue,
            String status,
            UUID approvedBy,
            Instant approvedAt,
            String note
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.importBatchId = importBatchId;
        this.openingDate = Objects.requireNonNull(openingDate, "openingDate must not be null");

        if (totalItems <= 0) {
            throw new IllegalArgumentException("totalItems must be greater than zero");
        }
        this.totalItems = totalItems;

        Objects.requireNonNull(totalQuantity, "totalQuantity must not be null");
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidStockQuantityException("totalQuantity must be strictly positive");
        }
        this.totalQuantity = totalQuantity.setScale(3, RoundingMode.HALF_UP);

        Objects.requireNonNull(totalValue, "totalValue must not be null");
        if (totalValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalValue must be non-negative");
        }
        this.totalValue = totalValue.setScale(2, RoundingMode.HALF_UP);

        this.status = status != null ? status : "COMMITTED";
        this.approvedBy = Objects.requireNonNull(approvedBy, "approvedBy must not be null");
        this.approvedAt = approvedAt != null ? approvedAt : Instant.now();
        this.note = note;
    }

    public static OpeningInventoryRecord createCommitted(
            UUID businessEntityId,
            UUID branchId,
            UUID importBatchId,
            LocalDate openingDate,
            int totalItems,
            BigDecimal totalQuantity,
            BigDecimal totalValue,
            UUID approvedBy,
            String note
    ) {
        return new OpeningInventoryRecord(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                importBatchId,
                openingDate,
                totalItems,
                totalQuantity,
                totalValue,
                "COMMITTED",
                approvedBy,
                Instant.now(),
                note
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public LocalDate getOpeningDate() {
        return openingDate;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public String getStatus() {
        return status;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public String getNote() {
        return note;
    }
}
