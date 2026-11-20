package com.mtms.movement.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * P1-MOV-01 / P1-MOV-02 / BR-MOV-01 / BR-MOV-02 / TC-027 / TC-028
 * Domain aggregate root for Intra-entity branch stock transfers.
 */
public class StockTransfer {

    private final UUID id;
    private final String transferNumber;
    private final UUID businessEntityId;
    private final UUID fromBranchId;
    private final UUID toBranchId;
    private final LocalDate requestedOn;
    private Instant dispatchedAt;
    private Instant completedAt;
    private TransferStatus status;
    private final UUID requestedBy;
    private UUID dispatchedBy;
    private UUID receivedBy;
    private int totalLines;
    private BigDecimal totalDispatchedQuantity;
    private BigDecimal totalReceivedQuantity;
    private BigDecimal totalTransitVarianceQuantity;
    private BigDecimal totalDispatchedCost;
    private BigDecimal totalReceivedCost;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<StockTransferLine> lines;

    public StockTransfer(
            UUID id,
            String transferNumber,
            UUID businessEntityId,
            UUID fromBranchId,
            UUID toBranchId,
            LocalDate requestedOn,
            Instant dispatchedAt,
            Instant completedAt,
            TransferStatus status,
            UUID requestedBy,
            UUID dispatchedBy,
            UUID receivedBy,
            int totalLines,
            BigDecimal totalDispatchedQuantity,
            BigDecimal totalReceivedQuantity,
            BigDecimal totalTransitVarianceQuantity,
            BigDecimal totalDispatchedCost,
            BigDecimal totalReceivedCost,
            String notes,
            Instant createdAt,
            Instant updatedAt,
            List<StockTransferLine> lines
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.transferNumber = Objects.requireNonNull(transferNumber, "transferNumber must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.fromBranchId = Objects.requireNonNull(fromBranchId, "fromBranchId must not be null");
        this.toBranchId = Objects.requireNonNull(toBranchId, "toBranchId must not be null");

        if (fromBranchId.equals(toBranchId)) {
            throw new IllegalArgumentException("fromBranchId and toBranchId must not be the same");
        }

        this.requestedOn = Objects.requireNonNull(requestedOn, "requestedOn must not be null");
        this.dispatchedAt = dispatchedAt;
        this.completedAt = completedAt;
        this.status = status != null ? status : TransferStatus.DRAFT;
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        this.dispatchedBy = dispatchedBy;
        this.receivedBy = receivedBy;
        this.totalLines = totalLines;
        this.totalDispatchedQuantity = (totalDispatchedQuantity != null ? totalDispatchedQuantity : BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
        this.totalReceivedQuantity = (totalReceivedQuantity != null ? totalReceivedQuantity : BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
        this.totalTransitVarianceQuantity = (totalTransitVarianceQuantity != null ? totalTransitVarianceQuantity : BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
        this.totalDispatchedCost = (totalDispatchedCost != null ? totalDispatchedCost : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        this.totalReceivedCost = (totalReceivedCost != null ? totalReceivedCost : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    public static StockTransfer createDraft(
            UUID id,
            String transferNumber,
            UUID businessEntityId,
            UUID fromBranchId,
            UUID toBranchId,
            LocalDate requestedOn,
            String notes,
            UUID requestedBy,
            List<StockTransferLine> lines
    ) {
        BigDecimal totalDispatchedQty = BigDecimal.ZERO;
        for (StockTransferLine line : lines) {
            totalDispatchedQty = totalDispatchedQty.add(line.getDispatchedBaseQty());
        }

        Instant now = Instant.now();
        return new StockTransfer(
                id,
                transferNumber,
                businessEntityId,
                fromBranchId,
                toBranchId,
                requestedOn,
                null,
                null,
                TransferStatus.DRAFT,
                requestedBy,
                null,
                null,
                lines.size(),
                totalDispatchedQty,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                notes,
                now,
                now,
                lines
        );
    }

    public void markDispatched(UUID dispatchedByUserId) {
        if (this.status != TransferStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT transfers can be dispatched. Current status: " + this.status);
        }
        this.status = TransferStatus.DISPATCHED;
        this.dispatchedBy = dispatchedByUserId;
        this.dispatchedAt = Instant.now();
        this.updatedAt = Instant.now();

        BigDecimal runningCost = BigDecimal.ZERO;
        for (StockTransferLine line : this.lines) {
            runningCost = runningCost.add(line.getDispatchedLineCost());
        }
        this.totalDispatchedCost = runningCost.setScale(2, RoundingMode.HALF_UP);
    }

    public void completeReceipt(UUID receivedByUserId) {
        if (this.status != TransferStatus.DISPATCHED) {
            throw new IllegalStateException("Only DISPATCHED transfers can be received. Current status: " + this.status);
        }

        BigDecimal sumReceivedQty = BigDecimal.ZERO;
        BigDecimal sumVarianceQty = BigDecimal.ZERO;
        BigDecimal sumReceivedCost = BigDecimal.ZERO;

        for (StockTransferLine line : this.lines) {
            sumReceivedQty = sumReceivedQty.add(line.getReceivedBaseQty());
            sumVarianceQty = sumVarianceQty.add(line.getTransitVarianceQty());
            sumReceivedCost = sumReceivedCost.add(line.getReceivedLineCost());
        }

        this.totalReceivedQuantity = sumReceivedQty.setScale(3, RoundingMode.HALF_UP);
        this.totalTransitVarianceQuantity = sumVarianceQty.setScale(3, RoundingMode.HALF_UP);
        this.totalReceivedCost = sumReceivedCost.setScale(2, RoundingMode.HALF_UP);
        this.receivedBy = receivedByUserId;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();

        // TC-027: If all quantity received -> COMPLETED, otherwise PARTIAL_RECEIPT (no vanish)
        if (this.totalReceivedQuantity.compareTo(this.totalDispatchedQuantity) == 0) {
            this.status = TransferStatus.COMPLETED;
        } else {
            this.status = TransferStatus.PARTIAL_RECEIPT;
        }
    }

    public void cancel(UUID actorUserId) {
        if (this.status != TransferStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT transfers can be cancelled. Current status: " + this.status);
        }
        this.status = TransferStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTransferNumber() {
        return transferNumber;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getFromBranchId() {
        return fromBranchId;
    }

    public UUID getToBranchId() {
        return toBranchId;
    }

    public LocalDate getRequestedOn() {
        return requestedOn;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public UUID getDispatchedBy() {
        return dispatchedBy;
    }

    public UUID getReceivedBy() {
        return receivedBy;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public BigDecimal getTotalDispatchedQuantity() {
        return totalDispatchedQuantity;
    }

    public BigDecimal getTotalReceivedQuantity() {
        return totalReceivedQuantity;
    }

    public BigDecimal getTotalTransitVarianceQuantity() {
        return totalTransitVarianceQuantity;
    }

    public BigDecimal getTotalDispatchedCost() {
        return totalDispatchedCost;
    }

    public BigDecimal getTotalReceivedCost() {
        return totalReceivedCost;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<StockTransferLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
