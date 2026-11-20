package com.mtms.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CatalogItem {

    private final UUID id;
    private final UUID businessEntityId;
    private final String itemCode;
    private String name;
    private final UUID baseUnitId;
    private ItemKind itemKind;
    private boolean lotTrackingRequired;
    private boolean expirationTrackingRequired;
    private ItemStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public CatalogItem(
            UUID id,
            UUID businessEntityId,
            String itemCode,
            String name,
            UUID baseUnitId,
            ItemKind itemKind,
            boolean lotTrackingRequired,
            boolean expirationTrackingRequired,
            ItemStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Item ID must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "Business entity ID must not be null");
        this.itemCode = Objects.requireNonNull(itemCode, "Item code must not be null").trim();
        this.name = Objects.requireNonNull(name, "Item name must not be null").trim();
        this.baseUnitId = Objects.requireNonNull(baseUnitId, "Base unit ID must not be null");
        this.itemKind = Objects.requireNonNull(itemKind, "Item kind must not be null");
        this.lotTrackingRequired = lotTrackingRequired;
        this.expirationTrackingRequired = expirationTrackingRequired;
        this.status = status != null ? status : ItemStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();

        validateTrackingInvariants();
    }

    public void update(String name, ItemKind itemKind, boolean lotTrackingRequired, boolean expirationTrackingRequired, ItemStatus status) {
        this.name = Objects.requireNonNull(name, "Item name must not be null").trim();
        this.itemKind = Objects.requireNonNull(itemKind, "Item kind must not be null");
        this.lotTrackingRequired = lotTrackingRequired;
        this.expirationTrackingRequired = expirationTrackingRequired;
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.updatedAt = Instant.now();

        validateTrackingInvariants();
    }

    private void validateTrackingInvariants() {
        if (expirationTrackingRequired && !lotTrackingRequired) {
            throw new InvalidTrackingInvariantException(
                    "Expiration tracking requires lot tracking to be enabled (itemCode: " + itemCode + ")"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getName() {
        return name;
    }

    public UUID getBaseUnitId() {
        return baseUnitId;
    }

    public ItemKind getItemKind() {
        return itemKind;
    }

    public boolean isLotTrackingRequired() {
        return lotTrackingRequired;
    }

    public boolean isExpirationTrackingRequired() {
        return expirationTrackingRequired;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogItem item)) return false;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
