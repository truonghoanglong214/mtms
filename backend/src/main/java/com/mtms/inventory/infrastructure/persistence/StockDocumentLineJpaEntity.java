package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockDocumentLine;
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
@Table(name = "stock_document_lines", schema = "mtms")
public class StockDocumentLineJpaEntity {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private StockDocumentJpaEntity document;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private CatalogItemJpaEntity catalogItem;

    @Column(name = "lot_id")
    private UUID lotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", insertable = false, updatable = false)
    private StockLotJpaEntity stockLot;

    @Column(name = "issue_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal issueQuantity;

    @Column(name = "booked_unit_cost", nullable = false, precision = 20, scale = 6)
    private BigDecimal bookedUnitCost;

    @Column(name = "booked_total", nullable = false, precision = 22, scale = 2)
    private BigDecimal bookedTotal;

    @Column(name = "notes")
    private String notes;

    public StockDocumentLineJpaEntity() {
    }

    public StockDocumentLineJpaEntity(
            UUID id,
            UUID documentId,
            int lineNo,
            UUID itemId,
            UUID lotId,
            BigDecimal issueQuantity,
            BigDecimal bookedUnitCost,
            BigDecimal bookedTotal,
            String notes
    ) {
        this.id = id;
        this.documentId = documentId;
        this.lineNo = lineNo;
        this.itemId = itemId;
        this.lotId = lotId;
        this.issueQuantity = issueQuantity;
        this.bookedUnitCost = bookedUnitCost;
        this.bookedTotal = bookedTotal;
        this.notes = notes;
    }

    public static StockDocumentLineJpaEntity fromDomain(StockDocumentLine line) {
        return new StockDocumentLineJpaEntity(
                line.getId(),
                line.getDocumentId(),
                line.getLineNo(),
                line.getItemId(),
                line.getLotId(),
                line.getIssueQuantity(),
                line.getBookedUnitCost(),
                line.getBookedTotal(),
                line.getNotes()
        );
    }

    public StockDocumentLine toDomain() {
        return new StockDocumentLine(
                id,
                documentId,
                lineNo,
                itemId,
                lotId,
                issueQuantity,
                bookedUnitCost,
                bookedTotal,
                notes
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocument(StockDocumentJpaEntity document) {
        this.document = document;
        if (document != null) {
            this.documentId = document.getId();
        }
    }

    public StockDocumentJpaEntity getDocument() {
        return document;
    }

    public int getLineNo() {
        return lineNo;
    }

    public UUID getItemId() {
        return itemId;
    }

    public CatalogItemJpaEntity getCatalogItem() {
        return catalogItem;
    }

    public UUID getLotId() {
        return lotId;
    }

    public StockLotJpaEntity getStockLot() {
        return stockLot;
    }

    public BigDecimal getIssueQuantity() {
        return issueQuantity;
    }

    public BigDecimal getBookedUnitCost() {
        return bookedUnitCost;
    }

    public BigDecimal getBookedTotal() {
        return bookedTotal;
    }

    public String getNotes() {
        return notes;
    }
}
