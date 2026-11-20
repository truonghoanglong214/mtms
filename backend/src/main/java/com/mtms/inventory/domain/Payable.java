package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-INV-03 / BR-INV-05 / TC-008
 * Domain entity representing branch debt to a supplier or counterparty.
 */
public class Payable {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private String counterpartyLabel;
    private String sourceType;
    private UUID sourceId;
    private LocalDate incurredDate;
    private LocalDate dueDate;
    private BigDecimal originalAmount;
    private BigDecimal paidAmount;
    private PayableStatus status;
    private final Instant createdAt;

    public Payable(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String counterpartyLabel,
            String sourceType,
            UUID sourceId,
            LocalDate incurredDate,
            LocalDate dueDate,
            BigDecimal originalAmount,
            BigDecimal paidAmount,
            PayableStatus status,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        setCounterpartyLabel(counterpartyLabel);
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceId = sourceId;
        this.incurredDate = Objects.requireNonNull(incurredDate, "incurredDate must not be null");
        this.dueDate = dueDate;
        setOriginalAmount(originalAmount);
        setPaidAmount(paidAmount != null ? paidAmount : BigDecimal.ZERO);
        this.status = status != null ? status : PayableStatus.OPEN;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        validateInvariants();
    }

    public static Payable createPurchaseReceiptDebt(
            UUID businessEntityId,
            UUID branchId,
            String supplierName,
            UUID receiptId,
            LocalDate receivedOn,
            LocalDate dueDate,
            BigDecimal totalAmount
    ) {
        return new Payable(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                supplierName,
                "PURCHASE_RECEIPT",
                receiptId,
                receivedOn,
                dueDate,
                totalAmount,
                BigDecimal.ZERO,
                PayableStatus.OPEN,
                Instant.now()
        );
    }

    private void validateInvariants() {
        if (this.paidAmount.compareTo(this.originalAmount) > 0) {
            throw new IllegalArgumentException("Paid amount cannot exceed original amount");
        }
    }

    public void setCounterpartyLabel(String counterpartyLabel) {
        if (counterpartyLabel == null || counterpartyLabel.isBlank()) {
            throw new IllegalArgumentException("Counterparty label must not be blank");
        }
        this.counterpartyLabel = counterpartyLabel.trim();
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        Objects.requireNonNull(originalAmount, "originalAmount must not be null");
        if (originalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Original amount cannot be negative");
        }
        this.originalAmount = originalAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        Objects.requireNonNull(paidAmount, "paidAmount must not be null");
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Paid amount cannot be negative");
        }
        this.paidAmount = paidAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public void setStatus(PayableStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public BigDecimal getRemainingAmount() {
        return this.originalAmount.subtract(this.paidAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getCounterpartyLabel() {
        return counterpartyLabel;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public LocalDate getIncurredDate() {
        return incurredDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public PayableStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
