package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-INV-02 / BR-INV-08
 * Domain entity representing an inventory lot tracked by lot code and optional expiration date.
 */
public class StockLot {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final UUID itemId;
    private final String lotCode;
    private final LocalDate expiresOn;
    private BigDecimal quantity;
    private Long version;
    private final Instant createdAt;

    public StockLot(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            UUID itemId,
            String lotCode,
            LocalDate expiresOn,
            BigDecimal quantity,
            Long version,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        if (lotCode == null || lotCode.isBlank()) {
            throw new MissingLotCodeException("Lot code cannot be null or blank");
        }
        this.lotCode = lotCode.trim();
        this.expiresOn = expiresOn;
        setQuantity(quantity);
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockLot createInitial(
            UUID businessEntityId,
            UUID branchId,
            UUID itemId,
            String lotCode,
            LocalDate expiresOn,
            BigDecimal quantity
    ) {
        return new StockLot(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                itemId,
                lotCode,
                expiresOn,
                quantity,
                0L,
                Instant.now()
        );
    }

    public void setQuantity(BigDecimal quantity) {
        Objects.requireNonNull(quantity, "quantity must not be null");
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidStockQuantityException("Lot quantity cannot be negative");
        }
        this.quantity = quantity.setScale(3, RoundingMode.HALF_UP);
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

    public String getLotCode() {
        return lotCode;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
