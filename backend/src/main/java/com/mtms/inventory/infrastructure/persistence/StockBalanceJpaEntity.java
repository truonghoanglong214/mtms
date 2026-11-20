package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_balances", schema = "mtms")
public class StockBalanceJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private CatalogItemJpaEntity catalogItem;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "inventory_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal inventoryValue;

    @Column(name = "avg_unit_cost", nullable = false, precision = 20, scale = 6)
    private BigDecimal avgUnitCost;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StockBalanceJpaEntity() {
    }

    public StockBalanceJpaEntity(
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
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.inventoryValue = inventoryValue;
        this.avgUnitCost = avgUnitCost;
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static StockBalanceJpaEntity fromDomain(StockBalance domain) {
        return new StockBalanceJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getItemId(),
                domain.getQuantity(),
                domain.getInventoryValue(),
                domain.getAvgUnitCost(),
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public StockBalance toDomain() {
        return new StockBalance(
                this.id,
                this.businessEntityId,
                this.branchId,
                this.itemId,
                this.quantity,
                this.inventoryValue,
                this.avgUnitCost,
                this.version,
                this.createdAt,
                this.updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public void setBusinessEntityId(UUID businessEntityId) {
        this.businessEntityId = businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public CatalogItemJpaEntity getCatalogItem() {
        return catalogItem;
    }

    public void setCatalogItem(CatalogItemJpaEntity catalogItem) {
        this.catalogItem = catalogItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getInventoryValue() {
        return inventoryValue;
    }

    public void setInventoryValue(BigDecimal inventoryValue) {
        this.inventoryValue = inventoryValue;
    }

    public BigDecimal getAvgUnitCost() {
        return avgUnitCost;
    }

    public void setAvgUnitCost(BigDecimal avgUnitCost) {
        this.avgUnitCost = avgUnitCost;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
