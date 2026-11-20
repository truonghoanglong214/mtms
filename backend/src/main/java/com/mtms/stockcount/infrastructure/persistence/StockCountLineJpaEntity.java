package com.mtms.stockcount.infrastructure.persistence;

import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.stockcount.domain.model.StockCountLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_count_lines", schema = "mtms")
public class StockCountLineJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCountJpaEntity stockCount;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private CatalogItemJpaEntity catalogItem;

    @Column(name = "lot_id")
    private UUID lotId;

    @Column(name = "expected_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal expectedQty;

    @Column(name = "counted_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal countedQty;

    @Column(name = "variance_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal varianceQty;

    @Column(name = "snapshot_unit_cost", nullable = false, precision = 20, scale = 6)
    private BigDecimal snapshotUnitCost;

    @Column(name = "variance_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal varianceValue;

    @Column(name = "discrepancy_reason")
    private String discrepancyReason;

    @Column(name = "notes")
    private String notes;

    public StockCountLineJpaEntity() {}

    public StockCountLine toDomain() {
        return new StockCountLine(
                id,
                stockCount != null ? stockCount.getId() : null,
                lineNumber,
                itemId,
                lotId,
                expectedQty,
                countedQty,
                varianceQty,
                snapshotUnitCost,
                varianceValue,
                discrepancyReason,
                notes
        );
    }

    public static StockCountLineJpaEntity fromDomain(StockCountLine domain, StockCountJpaEntity parent) {
        StockCountLineJpaEntity entity = new StockCountLineJpaEntity();
        entity.id = domain.getId();
        entity.stockCount = parent;
        entity.lineNumber = domain.getLineNumber();
        entity.itemId = domain.getItemId();
        entity.lotId = domain.getLotId();
        entity.expectedQty = domain.getExpectedQty();
        entity.countedQty = domain.getCountedQty();
        entity.varianceQty = domain.getVarianceQty();
        entity.snapshotUnitCost = domain.getSnapshotUnitCost();
        entity.varianceValue = domain.getVarianceValue();
        entity.discrepancyReason = domain.getDiscrepancyReason();
        entity.notes = domain.getNotes();
        return entity;
    }

    public UUID getId() { return id; }
    public StockCountJpaEntity getStockCount() { return stockCount; }
    public int getLineNumber() { return lineNumber; }
    public UUID getItemId() { return itemId; }
    public CatalogItemJpaEntity getCatalogItem() { return catalogItem; }
    public UUID getLotId() { return lotId; }
    public BigDecimal getExpectedQty() { return expectedQty; }
    public BigDecimal getCountedQty() { return countedQty; }
    public BigDecimal getVarianceQty() { return varianceQty; }
    public BigDecimal getSnapshotUnitCost() { return snapshotUnitCost; }
    public BigDecimal getVarianceValue() { return varianceValue; }
    public String getDiscrepancyReason() { return discrepancyReason; }
    public String getNotes() { return notes; }
}
