package com.mtms.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing branch-level threshold configurations for operating expenses.
 */
public class BranchExpenseConfig {

    public static final BigDecimal DEFAULT_ALERT_THRESHOLD = new BigDecimal("2000000.00");
    public static final BigDecimal DEFAULT_EVIDENCE_THRESHOLD = new BigDecimal("500000.00");

    private final UUID branchId;
    private BigDecimal alertThresholdAmount;
    private BigDecimal evidenceRequiredThreshold;
    private final Instant createdAt;
    private Instant updatedAt;

    public BranchExpenseConfig(
            UUID branchId,
            BigDecimal alertThresholdAmount,
            BigDecimal evidenceRequiredThreshold,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.branchId = Objects.requireNonNull(branchId, "Branch ID must not be null");
        this.alertThresholdAmount = alertThresholdAmount != null ? alertThresholdAmount : DEFAULT_ALERT_THRESHOLD;
        this.evidenceRequiredThreshold = evidenceRequiredThreshold != null ? evidenceRequiredThreshold : DEFAULT_EVIDENCE_THRESHOLD;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static BranchExpenseConfig defaultForBranch(UUID branchId) {
        Instant now = Instant.now();
        return new BranchExpenseConfig(branchId, DEFAULT_ALERT_THRESHOLD, DEFAULT_EVIDENCE_THRESHOLD, now, now);
    }

    public void updateThresholds(BigDecimal alertThresholdAmount, BigDecimal evidenceRequiredThreshold) {
        if (alertThresholdAmount != null && alertThresholdAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.alertThresholdAmount = alertThresholdAmount;
        }
        if (evidenceRequiredThreshold != null && evidenceRequiredThreshold.compareTo(BigDecimal.ZERO) > 0) {
            this.evidenceRequiredThreshold = evidenceRequiredThreshold;
        }
        this.updatedAt = Instant.now();
    }

    public boolean isReviewRequired(BigDecimal amount) {
        return amount != null && amount.compareTo(this.alertThresholdAmount) >= 0;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public BigDecimal getAlertThresholdAmount() {
        return alertThresholdAmount;
    }

    public BigDecimal getEvidenceRequiredThreshold() {
        return evidenceRequiredThreshold;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
