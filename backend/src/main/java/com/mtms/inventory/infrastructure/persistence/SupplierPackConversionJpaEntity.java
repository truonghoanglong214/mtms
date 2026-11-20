package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.SupplierPackConversion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "supplier_pack_conversions", schema = "mtms")
public class SupplierPackConversionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private SupplierJpaEntity supplier;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private CatalogItemJpaEntity catalogItem;

    @Column(name = "pack_name", nullable = false, length = 100)
    private String packName;

    @Column(name = "base_units_per_pack", nullable = false, precision = 18, scale = 3)
    private BigDecimal baseUnitsPerPack;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SupplierPackConversionJpaEntity() {
    }

    public SupplierPackConversionJpaEntity(
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
        this.id = id != null ? id : UUID.randomUUID();
        this.businessEntityId = businessEntityId;
        this.supplierId = supplierId;
        this.itemId = itemId;
        this.packName = packName;
        this.baseUnitsPerPack = baseUnitsPerPack;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static SupplierPackConversionJpaEntity fromDomain(SupplierPackConversion domain) {
        return new SupplierPackConversionJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getSupplierId(),
                domain.getItemId(),
                domain.getPackName(),
                domain.getBaseUnitsPerPack(),
                domain.getEffectiveFrom(),
                domain.getEffectiveTo(),
                domain.getCreatedAt()
        );
    }

    public SupplierPackConversion toDomain() {
        return new SupplierPackConversion(
                this.id,
                this.businessEntityId,
                this.supplierId,
                this.itemId,
                this.packName,
                this.baseUnitsPerPack,
                this.effectiveFrom,
                this.effectiveTo,
                this.createdAt
        );
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

    public SupplierJpaEntity getSupplier() {
        return supplier;
    }

    public UUID getItemId() {
        return itemId;
    }

    public CatalogItemJpaEntity getCatalogItem() {
        return catalogItem;
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
}
