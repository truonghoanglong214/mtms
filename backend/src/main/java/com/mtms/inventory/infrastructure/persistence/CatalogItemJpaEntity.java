package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.CatalogItem;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_items", schema = "mtms")
public class CatalogItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "base_unit_id", nullable = false)
    private UUID baseUnitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_unit_id", insertable = false, updatable = false)
    private UnitJpaEntity baseUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_kind", nullable = false, length = 24)
    private ItemKind itemKind;

    @Column(name = "lot_tracking_required", nullable = false)
    private boolean lotTrackingRequired;

    @Column(name = "expiration_tracking_required", nullable = false)
    private boolean expirationTrackingRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ItemStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogItemJpaEntity() {
    }

    public CatalogItemJpaEntity(
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
        this.id = id != null ? id : UUID.randomUUID();
        this.businessEntityId = businessEntityId;
        this.itemCode = itemCode;
        this.name = name;
        this.baseUnitId = baseUnitId;
        this.itemKind = itemKind;
        this.lotTrackingRequired = lotTrackingRequired;
        this.expirationTrackingRequired = expirationTrackingRequired;
        this.status = status != null ? status : ItemStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static CatalogItemJpaEntity fromDomain(CatalogItem domain) {
        return new CatalogItemJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getItemCode(),
                domain.getName(),
                domain.getBaseUnitId(),
                domain.getItemKind(),
                domain.isLotTrackingRequired(),
                domain.isExpirationTrackingRequired(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public CatalogItem toDomain() {
        return new CatalogItem(
                this.id,
                this.businessEntityId,
                this.itemCode,
                this.name,
                this.baseUnitId,
                this.itemKind,
                this.lotTrackingRequired,
                this.expirationTrackingRequired,
                this.status,
                this.createdAt,
                this.updatedAt
        );
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

    public UnitJpaEntity getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(UnitJpaEntity baseUnit) {
        this.baseUnit = baseUnit;
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

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
