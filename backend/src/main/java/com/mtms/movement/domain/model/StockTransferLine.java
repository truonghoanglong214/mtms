package com.mtms.movement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P1-MOV-01 / P1-MOV-02 / TC-027
 * Domain model representing a line item in a stock transfer.
 */
public class StockTransferLine {

    private final UUID id;
    private final UUID transferId;
    private final int lineNumber;
    private final UUID itemId;
    private final UUID lotId;
    private final BigDecimal dispatchedBaseQty;
    private BigDecimal receivedBaseQty;
    private BigDecimal transitVarianceQty;
    private BigDecimal senderUnitCost;
    private BigDecimal dispatchedLineCost;
    private BigDecimal receivedLineCost;
    private String receiverNote;
    private String notes;
    private final Instant createdAt;

    public StockTransferLine(
            UUID id,
            UUID transferId,
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
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.transferId = Objects.requireNonNull(transferId, "transferId must not be null");
        this.lineNumber = lineNumber;
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.lotId = lotId;
        this.dispatchedBaseQty = Objects.requireNonNull(dispatchedBaseQty, "dispatchedBaseQty must not be null")
                .setScale(3, RoundingMode.HALF_UP);
        this.receivedBaseQty = (receivedBaseQty != null ? receivedBaseQty : BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
        this.transitVarianceQty = (transitVarianceQty != null ? transitVarianceQty : BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
        this.senderUnitCost = (senderUnitCost != null ? senderUnitCost : BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        this.dispatchedLineCost = (dispatchedLineCost != null ? dispatchedLineCost : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        this.receivedLineCost = (receivedLineCost != null ? receivedLineCost : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        this.receiverNote = receiverNote;
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockTransferLine createDraftLine(
            UUID transferId,
            int lineNumber,
            UUID itemId,
            UUID lotId,
            BigDecimal dispatchedQty,
            String notes
    ) {
        if (dispatchedQty == null || dispatchedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Dispatched quantity must be strictly positive (> 0)");
        }
        return new StockTransferLine(
                UUID.randomUUID(),
                transferId,
                lineNumber,
                itemId,
                lotId,
                dispatchedQty,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                notes,
                Instant.now()
        );
    }

    public void dispatch(BigDecimal senderWac) {
        this.senderUnitCost = senderWac.setScale(6, RoundingMode.HALF_UP);
        this.dispatchedLineCost = this.dispatchedBaseQty.multiply(this.senderUnitCost)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void recordReceipt(BigDecimal receivedQty, String receiverNote) {
        Objects.requireNonNull(receivedQty, "receivedQty must not be null");
        BigDecimal scaledReceived = receivedQty.setScale(3, RoundingMode.HALF_UP);
        if (scaledReceived.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Received quantity cannot be negative");
        }
        if (scaledReceived.compareTo(this.dispatchedBaseQty) > 0) {
            throw new IllegalArgumentException("Received quantity cannot exceed dispatched quantity");
        }

        this.receivedBaseQty = scaledReceived;
        this.transitVarianceQty = this.dispatchedBaseQty.subtract(scaledReceived).setScale(3, RoundingMode.HALF_UP);
        this.receivedLineCost = scaledReceived.multiply(this.senderUnitCost).setScale(2, RoundingMode.HALF_UP);
        this.receiverNote = receiverNote;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
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

    public BigDecimal getReceivedBaseQty() {
        return receivedBaseQty;
    }

    public BigDecimal getTransitVarianceQty() {
        return transitVarianceQty;
    }

    public BigDecimal getSenderUnitCost() {
        return senderUnitCost;
    }

    public BigDecimal getDispatchedLineCost() {
        return dispatchedLineCost;
    }

    public BigDecimal getReceivedLineCost() {
        return receivedLineCost;
    }

    public String getReceiverNote() {
        return receiverNote;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
