package com.mtms.finance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-FIN-05
 * Domain entity representing a receivable (monies owed to the business).
 */
public class Receivable {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private String counterpartyLabel;
    private String sourceType;
    private UUID sourceId;
    private LocalDate incurredDate;
    private LocalDate dueDate;
    private BigDecimal originalAmount;
    private BigDecimal receivedAmount;
    private ReceivableStatus status;
    private String notes;
    private final UUID createdBy;
    private final Instant createdAt;

    public Receivable(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String counterpartyLabel,
            String sourceType,
            UUID sourceId,
            LocalDate incurredDate,
            LocalDate dueDate,
            BigDecimal originalAmount,
            BigDecimal receivedAmount,
            ReceivableStatus status,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.counterpartyLabel = Objects.requireNonNull(counterpartyLabel, "counterpartyLabel must not be null").trim();
        this.sourceType = sourceType != null ? sourceType : "MANUAL_RECEIVABLE";
        this.sourceId = sourceId;
        this.incurredDate = Objects.requireNonNull(incurredDate, "incurredDate must not be null");
        this.dueDate = dueDate;
        this.originalAmount = Objects.requireNonNull(originalAmount, "originalAmount must not be null").setScale(2, RoundingMode.HALF_UP);
        if (this.originalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Original amount cannot be negative");
        }
        this.receivedAmount = (receivedAmount != null ? receivedAmount : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (this.receivedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Received amount cannot be negative");
        }
        if (this.receivedAmount.compareTo(this.originalAmount) > 0) {
            throw new IllegalArgumentException("Received amount cannot exceed original amount");
        }
        this.status = status != null ? status : ReceivableStatus.OPEN;
        this.notes = notes;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public BigDecimal getRemainingAmount() {
        return originalAmount.subtract(receivedAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public UUID getBranchId() { return branchId; }
    public String getCounterpartyLabel() { return counterpartyLabel; }
    public String getSourceType() { return sourceType; }
    public UUID getSourceId() { return sourceId; }
    public LocalDate getIncurredDate() { return incurredDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public ReceivableStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public void setStatus(ReceivableStatus status) {
        this.status = status;
    }
}
