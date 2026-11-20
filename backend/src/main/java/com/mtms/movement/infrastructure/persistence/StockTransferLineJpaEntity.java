package com.mtms.movement.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_transfer_lines", schema = "mtms")
public class StockTransferLineJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransferJpaEntity transfer;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "lot_id")
    private UUID lotId;

    @Column(name = "dispatched_base_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal dispatchedBaseQty;

    @Column(name = "received_base_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal receivedBaseQty;

    @Column(name = "transit_variance_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal transitVarianceQty;

    @Column(name = "sender_unit_cost", nullable = false, precision = 20, scale = 6)
    private BigDecimal senderUnitCost;

    @Column(name = "dispatched_line_cost", nullable = false, precision = 20, scale = 2)
    private BigDecimal dispatchedLineCost;

    @Column(name = "received_line_cost", nullable = false, precision = 20, scale = 2)
    private BigDecimal receivedLineCost;

    @Column(name = "receiver_note", columnDefinition = "TEXT")
    private String receiverNote;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StockTransferLineJpaEntity() {}

    public StockTransferLineJpaEntity(
            UUID id,
            StockTransferJpaEntity transfer,
            int lineNumber,
            UUID itemId,
            UUID lotId,
            BigDecimal dispatchedBaseQty,
            BigDecimal receivedBaseQty,
            BigDecimal transitVarianceQty,
            BigDecimal senderUnitCost,
            BigDecimal dispatchedLineCost,
            BigDecimal receivedLineCost,
            String receiverNote,
            String notes,
            Instant createdAt
    ) {
        this.id = id;
        this.transfer = transfer;
        this.lineNumber = lineNumber;
        this.itemId = itemId;
        this.lotId = lotId;
        this.dispatchedBaseQty = dispatchedBaseQty;
        this.receivedBaseQty = receivedBaseQty;
        this.transitVarianceQty = transitVarianceQty;
        this.senderUnitCost = senderUnitCost;
        this.dispatchedLineCost = dispatchedLineCost;
        this.receivedLineCost = receivedLineCost;
        this.receiverNote = receiverNote;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public StockTransferJpaEntity getTransfer() {
        return transfer;
    }

    public void setTransfer(StockTransferJpaEntity transfer) {
        this.transfer = transfer;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getLotId() {
        return lotId;
    }

    public BigDecimal getDispatchedBaseQty() {
        return dispatchedBaseQty;
    }

    public void setDispatchedBaseQty(BigDecimal dispatchedBaseQty) {
        this.dispatchedBaseQty = dispatchedBaseQty;
    }

    public BigDecimal getReceivedBaseQty() {
        return receivedBaseQty;
    }

    public void setReceivedBaseQty(BigDecimal receivedBaseQty) {
        this.receivedBaseQty = receivedBaseQty;
    }

    public BigDecimal getTransitVarianceQty() {
        return transitVarianceQty;
    }

    public void setTransitVarianceQty(BigDecimal transitVarianceQty) {
        this.transitVarianceQty = transitVarianceQty;
    }

    public BigDecimal getSenderUnitCost() {
        return senderUnitCost;
    }

    public void setSenderUnitCost(BigDecimal senderUnitCost) {
        this.senderUnitCost = senderUnitCost;
    }

    public BigDecimal getDispatchedLineCost() {
        return dispatchedLineCost;
    }

    public void setDispatchedLineCost(BigDecimal dispatchedLineCost) {
        this.dispatchedLineCost = dispatchedLineCost;
    }

    public BigDecimal getReceivedLineCost() {
        return receivedLineCost;
    }

    public void setReceivedLineCost(BigDecimal receivedLineCost) {
        this.receivedLineCost = receivedLineCost;
    }

    public String getReceiverNote() {
        return receiverNote;
    }

    public void setReceiverNote(String receiverNote) {
        this.receiverNote = receiverNote;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
