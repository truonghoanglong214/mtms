package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockLot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stock_lots", schema = "mtms")
public class StockLotJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "lot_code", nullable = false, length = 80)
    private String lotCode;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StockLotJpaEntity() {
    }

    public StockLotJpaEntity(
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
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.itemId = itemId;
        this.lotCode = lotCode;
        this.expiresOn = expiresOn;
        this.quantity = quantity;
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockLotJpaEntity fromDomain(StockLot domain) {
        return new StockLotJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getItemId(),
                domain.getLotCode(),
                domain.getExpiresOn(),
                domain.getQuantity(),
                domain.getVersion(),
                domain.getCreatedAt()
        );
    }

    public StockLot toDomain() {
        return new StockLot(
                this.id,
                this.businessEntityId,
                this.branchId,
                this.itemId,
                this.lotCode,
                this.expiresOn,
                this.quantity,
                this.version,
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

    public String getLotCode() {
        return lotCode;
    }

    public void setLotCode(String lotCode) {
        this.lotCode = lotCode;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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
}
