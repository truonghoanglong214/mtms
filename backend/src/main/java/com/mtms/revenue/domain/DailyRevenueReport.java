package com.mtms.revenue.domain;

import com.mtms.common.decimal.MoneyUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-REV-02 / P0-REV-03 / BR-REV-01 / BR-REV-03 / TC-006
 * Domain aggregate root for branch aggregated daily revenue reporting.
 * Math invariant: totalRevenue = cashAmount + bankTransferAmount + deliveryAppNetAmount + otherAdjustmentAmount.
 * NO POS: only daily aggregates. Delivery-app NET is recorded in P&L, not automatic bank cash.
 */
public class DailyRevenueReport {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final LocalDate businessDate;
    private int revision;
    private boolean isCurrent;
    private BigDecimal cashAmount;
    private BigDecimal bankTransferAmount;
    private BigDecimal deliveryAppNetAmount;
    private BigDecimal otherAdjustmentAmount;
    private BigDecimal totalRevenue;
    private RevenueReportStatus status;
    private UUID submittedBy;
    private Instant submittedAt;
    private UUID confirmedBy;
    private Instant confirmedAt;
    private String adjustmentReason;
    private UUID evidenceFileId;
    private String note;
    private final Instant createdAt;
    private Instant updatedAt;

    public DailyRevenueReport(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            LocalDate businessDate,
            int revision,
            boolean isCurrent,
            BigDecimal cashAmount,
            BigDecimal bankTransferAmount,
            BigDecimal deliveryAppNetAmount,
            BigDecimal otherAdjustmentAmount,
            BigDecimal totalRevenue,
            RevenueReportStatus status,
            UUID submittedBy,
            Instant submittedAt,
            UUID confirmedBy,
            Instant confirmedAt,
            String adjustmentReason,
            UUID evidenceFileId,
            String note,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.businessDate = Objects.requireNonNull(businessDate, "businessDate must not be null");
        this.revision = Math.max(1, revision);
        this.isCurrent = isCurrent;
        this.submittedBy = Objects.requireNonNull(submittedBy, "submittedBy must not be null");
        this.submittedAt = submittedAt;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
        this.adjustmentReason = adjustmentReason;
        this.evidenceFileId = evidenceFileId;
        this.note = note;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.status = status != null ? status : RevenueReportStatus.DRAFT;

        setFinancialAmounts(cashAmount, bankTransferAmount, deliveryAppNetAmount, otherAdjustmentAmount, totalRevenue);
    }

    public static DailyRevenueReport create(
            UUID businessEntityId,
            UUID branchId,
            LocalDate businessDate,
            BigDecimal cashAmount,
            BigDecimal bankTransferAmount,
            BigDecimal deliveryAppNetAmount,
            BigDecimal otherAdjustmentAmount,
            BigDecimal explicitTotalRevenue,
            UUID evidenceFileId,
            String note,
            boolean submitImmediately,
            UUID actorUserId
    ) {
        Instant now = Instant.now();
        RevenueReportStatus initialStatus = submitImmediately ? RevenueReportStatus.SUBMITTED : RevenueReportStatus.DRAFT;
        Instant initialSubmittedAt = submitImmediately ? now : null;

        BigDecimal safeCash = cashAmount != null ? MoneyUtils.roundMoney(cashAmount) : MoneyUtils.ZERO_MONEY;
        BigDecimal safeBank = bankTransferAmount != null ? MoneyUtils.roundMoney(bankTransferAmount) : MoneyUtils.ZERO_MONEY;
        BigDecimal safeDelivery = deliveryAppNetAmount != null ? MoneyUtils.roundMoney(deliveryAppNetAmount) : MoneyUtils.ZERO_MONEY;
        BigDecimal safeOther = otherAdjustmentAmount != null ? MoneyUtils.roundMoney(otherAdjustmentAmount) : MoneyUtils.ZERO_MONEY;
        BigDecimal calculatedTotal = safeCash.add(safeBank).add(safeDelivery).add(safeOther);

        BigDecimal effectiveTotal = explicitTotalRevenue != null ? MoneyUtils.roundMoney(explicitTotalRevenue) : calculatedTotal;

        return new DailyRevenueReport(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                businessDate,
                1,
                true,
                safeCash,
                safeBank,
                safeDelivery,
                safeOther,
                effectiveTotal,
                initialStatus,
                actorUserId,
                initialSubmittedAt,
                null,
                null,
                null,
                evidenceFileId,
                note,
                now,
                now
        );
    }

    public void setFinancialAmounts(
            BigDecimal cash,
            BigDecimal bank,
            BigDecimal delivery,
            BigDecimal other,
            BigDecimal total
    ) {
        BigDecimal safeCash = cash != null ? MoneyUtils.roundMoney(cash) : MoneyUtils.ZERO_MONEY;
        BigDecimal safeBank = bank != null ? MoneyUtils.roundMoney(bank) : MoneyUtils.ZERO_MONEY;
        BigDecimal safeDelivery = delivery != null ? MoneyUtils.roundMoney(delivery) : MoneyUtils.ZERO_MONEY;
        BigDecimal safeOther = other != null ? MoneyUtils.roundMoney(other) : MoneyUtils.ZERO_MONEY;

        if (safeCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRevenueMathException("Cash amount cannot be negative");
        }
        if (safeBank.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRevenueMathException("Bank transfer amount cannot be negative");
        }
        if (safeDelivery.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRevenueMathException("Delivery app net amount cannot be negative");
        }

        BigDecimal expectedTotal = safeCash.add(safeBank).add(safeDelivery).add(safeOther);
        BigDecimal safeTotal = total != null ? MoneyUtils.roundMoney(total) : expectedTotal;

        if (safeTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRevenueMathException("Total revenue cannot be negative");
        }
        if (safeTotal.compareTo(expectedTotal) != 0) {
            throw new InvalidRevenueMathException(
                    "Total revenue (" + safeTotal + ") does not match sum of components (" + expectedTotal + ")"
            );
        }

        this.cashAmount = safeCash;
        this.bankTransferAmount = safeBank;
        this.deliveryAppNetAmount = safeDelivery;
        this.otherAdjustmentAmount = safeOther;
        this.totalRevenue = safeTotal;
        this.updatedAt = Instant.now();
    }

    public void submit(UUID actorUserId) {
        if (this.status == RevenueReportStatus.CONFIRMED) {
            throw new ReportAlreadyConfirmedException("Report is already confirmed and cannot be submitted again");
        }
        this.status = RevenueReportStatus.SUBMITTED;
        this.submittedBy = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        this.submittedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void confirm(UUID actorUserId, String reason) {
        if (this.status == RevenueReportStatus.CONFIRMED) {
            // TC-006: Idempotent confirmation - already confirmed, do nothing
            return;
        }
        this.status = RevenueReportStatus.CONFIRMED;
        this.confirmedBy = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        this.confirmedAt = Instant.now();
        if (reason != null && !reason.trim().isEmpty()) {
            this.adjustmentReason = reason.trim();
        }
        this.updatedAt = Instant.now();
    }

    public void markSuperseded() {
        this.isCurrent = false;
        this.status = RevenueReportStatus.SUPERSEDED;
        this.updatedAt = Instant.now();
    }

    public DailyRevenueReport createAdjustmentRevision(
            BigDecimal newCash,
            BigDecimal newBank,
            BigDecimal newDelivery,
            BigDecimal newOther,
            BigDecimal newTotal,
            String reason,
            UUID evidenceId,
            String newNote,
            UUID actorUserId
    ) {
        this.markSuperseded();

        Instant now = Instant.now();
        BigDecimal effectiveCash = newCash != null ? newCash : this.cashAmount;
        BigDecimal effectiveBank = newBank != null ? newBank : this.bankTransferAmount;
        BigDecimal effectiveDelivery = newDelivery != null ? newDelivery : this.deliveryAppNetAmount;
        BigDecimal effectiveOther = newOther != null ? newOther : this.otherAdjustmentAmount;
        UUID effectiveEvidence = evidenceId != null ? evidenceId : this.evidenceFileId;
        String effectiveNote = newNote != null ? newNote : this.note;

        DailyRevenueReport nextReport = new DailyRevenueReport(
                UUID.randomUUID(),
                this.businessEntityId,
                this.branchId,
                this.businessDate,
                this.revision + 1,
                true,
                effectiveCash,
                effectiveBank,
                effectiveDelivery,
                effectiveOther,
                newTotal,
                RevenueReportStatus.CONFIRMED,
                this.submittedBy,
                this.submittedAt != null ? this.submittedAt : now,
                actorUserId,
                now,
                reason,
                effectiveEvidence,
                effectiveNote,
                now,
                now
        );
        return nextReport;
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

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public int getRevision() {
        return revision;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public BigDecimal getCashAmount() {
        return cashAmount;
    }

    public BigDecimal getBankTransferAmount() {
        return bankTransferAmount;
    }

    public BigDecimal getDeliveryAppNetAmount() {
        return deliveryAppNetAmount;
    }

    public BigDecimal getOtherAdjustmentAmount() {
        return otherAdjustmentAmount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public RevenueReportStatus getStatus() {
        return status;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
