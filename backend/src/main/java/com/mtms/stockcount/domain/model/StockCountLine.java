package com.mtms.stockcount.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public class StockCountLine {

    private final UUID id;
    private final UUID stockCountId;
    private final int lineNumber;
    private final UUID itemId;
    private final UUID lotId;
    private final BigDecimal expectedQty;
    private BigDecimal countedQty;
    private BigDecimal varianceQty;
    private final BigDecimal snapshotUnitCost;
    private BigDecimal varianceValue;
    private String discrepancyReason;
    private String notes;

    public StockCountLine(
            UUID id,
            UUID stockCountId,
            int lineNumber,
            UUID itemId,
            UUID lotId,
            BigDecimal expectedQty,
            BigDecimal countedQty,
            BigDecimal varianceQty,
            BigDecimal snapshotUnitCost,
            BigDecimal varianceValue,
            String discrepancyReason,
            String notes
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.stockCountId = Objects.requireNonNull(stockCountId, "stockCountId must not be null");
        this.lineNumber = lineNumber;
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.lotId = lotId;
        this.expectedQty = Objects.requireNonNull(expectedQty, "expectedQty must not be null");
        this.countedQty = Objects.requireNonNull(countedQty, "countedQty must not be null");
        this.varianceQty = varianceQty != null ? varianceQty : this.countedQty.subtract(this.expectedQty);
        this.snapshotUnitCost = snapshotUnitCost != null ? snapshotUnitCost : BigDecimal.ZERO;
        this.varianceValue = varianceValue != null ? varianceValue : this.varianceQty.multiply(this.snapshotUnitCost).setScale(2, RoundingMode.HALF_UP);
        this.discrepancyReason = discrepancyReason;
        this.notes = notes;
    }

    public static StockCountLine createInitial(
            UUID stockCountId,
            int lineNumber,
            UUID itemId,
            UUID lotId,
            BigDecimal expectedQty,
            BigDecimal snapshotUnitCost
    ) {
        BigDecimal initialCounted = expectedQty != null ? expectedQty : BigDecimal.ZERO;
        return new StockCountLine(
                UUID.randomUUID(),
                stockCountId,
                lineNumber,
                itemId,
                lotId,
                initialCounted,
                initialCounted,
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                snapshotUnitCost != null ? snapshotUnitCost : BigDecimal.ZERO,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                null,
                null
        );
    }

    public void updateCountedQty(BigDecimal newCountedQty, String reason, String notes) {
        if (newCountedQty == null || newCountedQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số lượng thực đếm không được âm");
        }
        this.countedQty = newCountedQty;
        this.varianceQty = this.countedQty.subtract(this.expectedQty);
        this.varianceValue = this.varianceQty.multiply(this.snapshotUnitCost).setScale(2, RoundingMode.HALF_UP);
        this.discrepancyReason = reason;
        if (notes != null) {
            this.notes = notes;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getStockCountId() {
        return stockCountId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getLotId() {
        return lotId;
    }

    public BigDecimal getExpectedQty() {
        return expectedQty;
    }

    public BigDecimal getCountedQty() {
        return countedQty;
    }

    public BigDecimal getVarianceQty() {
        return varianceQty;
    }

    public BigDecimal getSnapshotUnitCost() {
        return snapshotUnitCost;
    }

    public BigDecimal getVarianceValue() {
        return varianceValue;
    }

    public String getDiscrepancyReason() {
        return discrepancyReason;
    }

    public String getNotes() {
        return notes;
    }
}
