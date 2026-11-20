package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-INV-02 / BR-INV-08
 * Domain entity representing an immutable stock ledger audit record.
 */
public class StockMovement {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final UUID itemId;
    private final UUID lotId;
    private final LocalDate businessDate;
    private final StockMovementType movementType;
    private final BigDecimal signedQuantity;
    private final BigDecimal signedValue;
    private final BigDecimal bookedAvgCost;
    private final String sourceType;
    private final UUID sourceId;
    private final String sourceRef;
    private final UUID createdBy;
    private final Instant createdAt;

    public StockMovement(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            UUID itemId,
            UUID lotId,
            LocalDate businessDate,
            StockMovementType movementType,
            BigDecimal signedQuantity,
            BigDecimal signedValue,
            BigDecimal bookedAvgCost,
            String sourceType,
            UUID sourceId,
            String sourceRef,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.lotId = lotId;
        this.businessDate = Objects.requireNonNull(businessDate, "businessDate must not be null");
        this.movementType = Objects.requireNonNull(movementType, "movementType must not be null");

        Objects.requireNonNull(signedQuantity, "signedQuantity must not be null");
        if (signedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("signedQuantity must not be zero");
        }
        this.signedQuantity = signedQuantity.setScale(3, RoundingMode.HALF_UP);

        Objects.requireNonNull(signedValue, "signedValue must not be null");
        this.signedValue = signedValue.setScale(2, RoundingMode.HALF_UP);

        Objects.requireNonNull(bookedAvgCost, "bookedAvgCost must not be null");
        if (bookedAvgCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("bookedAvgCost must be non-negative");
        }
        this.bookedAvgCost = bookedAvgCost.setScale(6, RoundingMode.HALF_UP);

        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
        this.sourceRef = sourceRef;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockMovement createOpeningBalanceMovement(
            UUID businessEntityId,
            UUID branchId,
            UUID itemId,
            UUID lotId,
            LocalDate businessDate,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID sourceId,
            String sourceRef,
            UUID createdBy
    ) {
        BigDecimal signedQty = quantity.setScale(3, RoundingMode.HALF_UP);
        BigDecimal cost = unitCost.setScale(6, RoundingMode.HALF_UP);
        BigDecimal signedVal = signedQty.multiply(cost).setScale(2, RoundingMode.HALF_UP);

        return new StockMovement(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                itemId,
                lotId,
                businessDate,
                StockMovementType.OPENING_BALANCE,
                signedQty,
                signedVal,
                cost,
                "OPENING_INVENTORY_RECORD",
                sourceId,
                sourceRef,
                createdBy,
                Instant.now()
        );
    }

    public static StockMovement createPurchaseReceiptMovement(
            UUID businessEntityId,
            UUID branchId,
            UUID itemId,
            UUID lotId,
            LocalDate businessDate,
            BigDecimal quantity,
            BigDecimal lineTotal,
            BigDecimal unitCost,
            UUID receiptId,
            String receiptNumber,
            UUID createdBy
    ) {
        BigDecimal signedQty = quantity.setScale(3, RoundingMode.HALF_UP);
        BigDecimal cost = unitCost.setScale(6, RoundingMode.HALF_UP);
        BigDecimal signedVal = lineTotal.setScale(2, RoundingMode.HALF_UP);

        return new StockMovement(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                itemId,
                lotId,
                businessDate,
                StockMovementType.PURCHASE_RECEIPT,
                signedQty,
                signedVal,
                cost,
                "PURCHASE_RECEIPT",
                receiptId,
                receiptNumber,
                createdBy,
                Instant.now()
        );
    }

    public static StockMovement createManualIssueMovement(
            UUID businessEntityId,
            UUID branchId,
            UUID itemId,
            UUID lotId,
            LocalDate businessDate,
            BigDecimal quantity,
            BigDecimal bookedTotal,
            BigDecimal bookedAvgCost,
            UUID documentId,
            String documentNumber,
            UUID createdBy
    ) {
        BigDecimal signedQty = quantity.negate().setScale(3, RoundingMode.HALF_UP);
        BigDecimal signedVal = bookedTotal.negate().setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = bookedAvgCost.setScale(6, RoundingMode.HALF_UP);

        return new StockMovement(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                itemId,
                lotId,
                businessDate,
                StockMovementType.MANUAL_ISSUE,
                signedQty,
                signedVal,
                cost,
                "STOCK_DOCUMENT",
                documentId,
                documentNumber,
                createdBy,
                Instant.now()
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

    public UUID getItemId() {
        return itemId;
    }

    public UUID getLotId() {
        return lotId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public BigDecimal getSignedQuantity() {
        return signedQuantity;
    }

    public BigDecimal getSignedValue() {
        return signedValue;
    }

    public BigDecimal getBookedAvgCost() {
        return bookedAvgCost;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
