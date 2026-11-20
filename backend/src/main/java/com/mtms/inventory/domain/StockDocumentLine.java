package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-02 / BR-INV-03
 * Domain entity representing an individual item line in a stock document.
 */
public class StockDocumentLine {

    private final UUID id;
    private final UUID documentId;
    private final int lineNo;
    private final UUID itemId;
    private final UUID lotId;
    private final BigDecimal issueQuantity;
    private BigDecimal bookedUnitCost;
    private BigDecimal bookedTotal;
    private final String notes;

    public StockDocumentLine(
            UUID id,
            UUID documentId,
            int lineNo,
            UUID itemId,
            UUID lotId,
            BigDecimal issueQuantity,
            BigDecimal bookedUnitCost,
            BigDecimal bookedTotal,
            String notes
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.documentId = Objects.requireNonNull(documentId, "documentId must not be null");
        if (lineNo <= 0) {
            throw new IllegalArgumentException("lineNo must be > 0");
        }
        this.lineNo = lineNo;
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.lotId = lotId;

        Objects.requireNonNull(issueQuantity, "issueQuantity must not be null");
        if (issueQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidIssueQuantityException("Issue quantity must be strictly positive (> 0)");
        }
        this.issueQuantity = issueQuantity.setScale(3, RoundingMode.HALF_UP);

        this.bookedUnitCost = bookedUnitCost != null
                ? bookedUnitCost.setScale(6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        this.bookedTotal = bookedTotal != null
                ? bookedTotal.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.notes = notes;
    }

    public void setBookedCost(BigDecimal unitCost, BigDecimal total) {
        Objects.requireNonNull(unitCost, "unitCost must not be null");
        Objects.requireNonNull(total, "total must not be null");
        if (unitCost.compareTo(BigDecimal.ZERO) < 0 || total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Booked cost cannot be negative");
        }
        this.bookedUnitCost = unitCost.setScale(6, RoundingMode.HALF_UP);
        this.bookedTotal = total.setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public int getLineNo() {
        return lineNo;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getLotId() {
        return lotId;
    }

    public BigDecimal getIssueQuantity() {
        return issueQuantity;
    }

    public BigDecimal getBookedUnitCost() {
        return bookedUnitCost;
    }

    public BigDecimal getBookedTotal() {
        return bookedTotal;
    }

    public String getNotes() {
        return notes;
    }
}
