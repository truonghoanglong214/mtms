package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockMovement;
import com.mtms.inventory.domain.StockMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stock_movements", schema = "mtms")
public class StockMovementJpaEntity {

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

    @Column(name = "lot_id")
    private UUID lotId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 28)
    private StockMovementType movementType;

    @Column(name = "signed_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal signedQuantity;

    @Column(name = "signed_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal signedValue;

    @Column(name = "booked_avg_cost", nullable = false, precision = 20, scale = 6)
    private BigDecimal bookedAvgCost;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "source_ref", length = 120)
    private String sourceRef;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StockMovementJpaEntity() {
    }

    public StockMovementJpaEntity(
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
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.itemId = itemId;
        this.lotId = lotId;
        this.businessDate = businessDate;
        this.movementType = movementType;
        this.signedQuantity = signedQuantity;
        this.signedValue = signedValue;
        this.bookedAvgCost = bookedAvgCost;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceRef = sourceRef;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockMovementJpaEntity fromDomain(StockMovement domain) {
        return new StockMovementJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getItemId(),
                domain.getLotId(),
                domain.getBusinessDate(),
                domain.getMovementType(),
                domain.getSignedQuantity(),
                domain.getSignedValue(),
                domain.getBookedAvgCost(),
                domain.getSourceType(),
                domain.getSourceId(),
                domain.getSourceRef(),
                domain.getCreatedBy(),
                domain.getCreatedAt()
        );
    }

    public StockMovement toDomain() {
        return new StockMovement(
                this.id,
                this.businessEntityId,
                this.branchId,
                this.itemId,
                this.lotId,
                this.businessDate,
                this.movementType,
                this.signedQuantity,
                this.signedValue,
                this.bookedAvgCost,
                this.sourceType,
                this.sourceId,
                this.sourceRef,
                this.createdBy,
                this.createdAt
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

    public UUID getLotId() {
        return lotId;
    }

    public void setLotId(UUID lotId) {
        this.lotId = lotId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(StockMovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getSignedQuantity() {
        return signedQuantity;
    }

    public void setSignedQuantity(BigDecimal signedQuantity) {
        this.signedQuantity = signedQuantity;
    }

    public BigDecimal getSignedValue() {
        return signedValue;
    }

    public void setSignedValue(BigDecimal signedValue) {
        this.signedValue = signedValue;
    }

    public BigDecimal getBookedAvgCost() {
        return bookedAvgCost;
    }

    public void setBookedAvgCost(BigDecimal bookedAvgCost) {
        this.bookedAvgCost = bookedAvgCost;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
