package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class SupplierPackConversion {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID supplierId;
    private final UUID itemId;
    private final String packName;
    private final BigDecimal baseUnitsPerPack;
    private final LocalDate effectiveFrom;
    private final LocalDate effectiveTo;
    private final Instant createdAt;

    public SupplierPackConversion(
            UUID id,
            UUID businessEntityId,
            UUID supplierId,
            UUID itemId,
            String packName,
            BigDecimal baseUnitsPerPack,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Conversion ID must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "Business entity ID must not be null");
        this.supplierId = Objects.requireNonNull(supplierId, "Supplier ID must not be null");
        this.itemId = Objects.requireNonNull(itemId, "Item ID must not be null");
        this.packName = Objects.requireNonNull(packName, "Pack name must not be null").trim();
        this.baseUnitsPerPack = Objects.requireNonNull(baseUnitsPerPack, "Base units per pack must not be null");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "Effective from date must not be null");
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt != null ? createdAt : Instant.now();

        validateConversionInvariants();
    }

    private void validateConversionInvariants() {
        if (baseUnitsPerPack.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidConversionRateException("Base units per pack must be greater than zero: " + baseUnitsPerPack);
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new InvalidConversionRateException(
                    "effectiveTo (" + effectiveTo + ") cannot be before effectiveFrom (" + effectiveFrom + ")"
            );
        }
    }

    public boolean isActiveOn(LocalDate date) {
        if (date == null) {
            return false;
        }
        if (date.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getPackName() {
        return packName;
    }

    public BigDecimal getBaseUnitsPerPack() {
        return baseUnitsPerPack;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplierPackConversion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
