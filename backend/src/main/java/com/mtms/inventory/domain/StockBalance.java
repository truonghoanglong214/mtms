package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-INV-02 / BR-INV-08
 * Domain entity representing on-hand stock and moving weighted average cost (WAC) per branch and item.
 */
public class StockBalance {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final UUID itemId;
    private BigDecimal quantity;
    private BigDecimal inventoryValue;
    private BigDecimal avgUnitCost;
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public StockBalance(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            UUID itemId,
            BigDecimal quantity,
            BigDecimal inventoryValue,
            BigDecimal avgUnitCost,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        setQuantity(quantity);
        setInventoryValue(inventoryValue);
        setAvgUnitCost(avgUnitCost);
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static StockBalance createInitial(
            UUID businessEntityId,
            UUID branchId,
            UUID itemId,
            BigDecimal quantity,
            BigDecimal unitCost
    ) {
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(unitCost, "unitCost must not be null");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidStockQuantityException("Initial opening quantity must be strictly positive (> 0)");
        }
        if (unitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit cost must be non-negative (>= 0)");
        }

        BigDecimal scaledQty = quantity.setScale(3, RoundingMode.HALF_UP);
        BigDecimal scaledCost = unitCost.setScale(6, RoundingMode.HALF_UP);
        BigDecimal totalValue = scaledQty.multiply(scaledCost).setScale(2, RoundingMode.HALF_UP);

        Instant now = Instant.now();
        return new StockBalance(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                itemId,
                scaledQty,
                totalValue,
                scaledCost,
                0L,
                now,
                now
        );
    }

    public void receiveGoods(BigDecimal receivedQty, BigDecimal lineTotal) {
        Objects.requireNonNull(receivedQty, "receivedQty must not be null");
        Objects.requireNonNull(lineTotal, "lineTotal must not be null");
        if (receivedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidStockQuantityException("Received quantity must be strictly positive (> 0)");
        }
        if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Line total must be non-negative");
        }
        BigDecimal newQty = this.quantity.add(receivedQty).setScale(3, RoundingMode.HALF_UP);
        BigDecimal newVal = this.inventoryValue.add(lineTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newAvgCost = newVal.divide(newQty, 6, RoundingMode.HALF_UP);

        this.quantity = newQty;
        this.inventoryValue = newVal;
        this.avgUnitCost = newAvgCost;
        this.updatedAt = Instant.now();
    }

    public BigDecimal issueStock(BigDecimal issueQty) {
        Objects.requireNonNull(issueQty, "issueQty must not be null");
        if (issueQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidIssueQuantityException("Issue quantity must be strictly positive (> 0)");
        }
        BigDecimal scaledIssueQty = issueQty.setScale(3, RoundingMode.HALF_UP);
        if (this.quantity.compareTo(scaledIssueQty) < 0) {
            throw new InsufficientStockException(
                    "Insufficient stock balance. Requested: " + scaledIssueQty + ", Available: " + this.quantity
            );
        }

        // TC-020: Booked unit cost is current Moving WAC
        BigDecimal bookedUnitCost = this.avgUnitCost;
        BigDecimal bookedTotal = scaledIssueQty.multiply(bookedUnitCost).setScale(2, RoundingMode.HALF_UP);

        BigDecimal remainingQty = this.quantity.subtract(scaledIssueQty).setScale(3, RoundingMode.HALF_UP);
        BigDecimal remainingValue = this.inventoryValue.subtract(bookedTotal).setScale(2, RoundingMode.HALF_UP);

        if (remainingQty.compareTo(BigDecimal.ZERO) == 0) {
            remainingValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if (remainingValue.compareTo(BigDecimal.ZERO) < 0) {
            remainingValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        this.quantity = remainingQty;
        this.inventoryValue = remainingValue;
        // WAC avgUnitCost remains unchanged during stock consumption
        this.updatedAt = Instant.now();

        return bookedTotal;
    }

    public void setQuantity(BigDecimal quantity) {
        Objects.requireNonNull(quantity, "quantity must not be null");
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidStockQuantityException("Quantity cannot be negative");
        }
        this.quantity = quantity.setScale(3, RoundingMode.HALF_UP);
    }

    public void setInventoryValue(BigDecimal inventoryValue) {
        Objects.requireNonNull(inventoryValue, "inventoryValue must not be null");
        if (inventoryValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Inventory value cannot be negative");
        }
        this.inventoryValue = inventoryValue.setScale(2, RoundingMode.HALF_UP);
    }

    public void setAvgUnitCost(BigDecimal avgUnitCost) {
        Objects.requireNonNull(avgUnitCost, "avgUnitCost must not be null");
        if (avgUnitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Average unit cost cannot be negative");
        }
        this.avgUnitCost = avgUnitCost.setScale(6, RoundingMode.HALF_UP);
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getInventoryValue() {
        return inventoryValue;
    }

    public BigDecimal getAvgUnitCost() {
        return avgUnitCost;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
