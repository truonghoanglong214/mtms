package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-INV-03 / BR-INV-05
 * Domain entity representing an individual item line in a purchase goods receipt.
 */
public class PurchaseReceiptLine {

    private final UUID id;
    private final UUID receiptId;
    private final int lineNo;
    private final UUID itemId;
    private final String packName;
    private final BigDecimal declaredPackQuantity;
    private final BigDecimal conversionSnapshot;
    private final BigDecimal receivedBaseQuantity;
    private final BigDecimal baseUnitCost;
    private final BigDecimal lineTotal;
    private final String lotCode;
    private final LocalDate expiresOn;

    public PurchaseReceiptLine(
            UUID id,
            UUID receiptId,
            int lineNo,
            UUID itemId,
            String packName,
            BigDecimal declaredPackQuantity,
            BigDecimal conversionSnapshot,
            BigDecimal receivedBaseQuantity,
            BigDecimal baseUnitCost,
            BigDecimal lineTotal,
            String lotCode,
            LocalDate expiresOn
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.receiptId = Objects.requireNonNull(receiptId, "receiptId must not be null");
        if (lineNo <= 0) {
            throw new IllegalArgumentException("lineNo must be strictly positive (> 0)");
        }
        this.lineNo = lineNo;
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.packName = packName != null && !packName.isBlank() ? packName.trim() : null;

        if (declaredPackQuantity != null && declaredPackQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("declaredPackQuantity must be strictly positive (> 0)");
        }
        this.declaredPackQuantity = declaredPackQuantity != null ? declaredPackQuantity.setScale(3, RoundingMode.HALF_UP) : null;

        BigDecimal conv = conversionSnapshot != null ? conversionSnapshot : BigDecimal.ONE;
        if (conv.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("conversionSnapshot must be strictly positive (> 0)");
        }
        this.conversionSnapshot = conv.setScale(6, RoundingMode.HALF_UP);

        Objects.requireNonNull(receivedBaseQuantity, "receivedBaseQuantity must not be null");
        if (receivedBaseQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReceiptQuantityException("Received base quantity must be strictly positive (> 0)");
        }
        this.receivedBaseQuantity = receivedBaseQuantity.setScale(3, RoundingMode.HALF_UP);

        Objects.requireNonNull(baseUnitCost, "baseUnitCost must not be null");
        if (baseUnitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("baseUnitCost must be non-negative (>= 0)");
        }
        this.baseUnitCost = baseUnitCost.setScale(6, RoundingMode.HALF_UP);

        BigDecimal calculatedTotal = lineTotal != null
                ? lineTotal.setScale(2, RoundingMode.HALF_UP)
                : this.receivedBaseQuantity.multiply(this.baseUnitCost).setScale(2, RoundingMode.HALF_UP);
        if (calculatedTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("lineTotal must be non-negative (>= 0)");
        }
        this.lineTotal = calculatedTotal;

        this.lotCode = lotCode != null && !lotCode.isBlank() ? lotCode.trim() : null;
        this.expiresOn = expiresOn;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReceiptId() {
        return receiptId;
    }

    public int getLineNo() {
        return lineNo;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getPackName() {
        return packName;
    }

    public BigDecimal getDeclaredPackQuantity() {
        return declaredPackQuantity;
    }

    public BigDecimal getConversionSnapshot() {
        return conversionSnapshot;
    }

    public BigDecimal getReceivedBaseQuantity() {
        return receivedBaseQuantity;
    }

    public BigDecimal getBaseUnitCost() {
        return baseUnitCost;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public String getLotCode() {
        return lotCode;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }
}
