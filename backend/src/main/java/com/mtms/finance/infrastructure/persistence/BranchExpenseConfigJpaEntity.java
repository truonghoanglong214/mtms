package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.BranchExpenseConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "branch_expense_configs", schema = "mtms")
public class BranchExpenseConfigJpaEntity {

    @Id
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "alert_threshold_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal alertThresholdAmount;

    @Column(name = "evidence_required_threshold", nullable = false, precision = 20, scale = 2)
    private BigDecimal evidenceRequiredThreshold;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BranchExpenseConfigJpaEntity() {
    }

    public BranchExpenseConfigJpaEntity(
            UUID branchId,
            BigDecimal alertThresholdAmount,
            BigDecimal evidenceRequiredThreshold,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.branchId = branchId;
        this.alertThresholdAmount = alertThresholdAmount != null ? alertThresholdAmount : BranchExpenseConfig.DEFAULT_ALERT_THRESHOLD;
        this.evidenceRequiredThreshold = evidenceRequiredThreshold != null ? evidenceRequiredThreshold : BranchExpenseConfig.DEFAULT_EVIDENCE_THRESHOLD;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static BranchExpenseConfigJpaEntity fromDomain(BranchExpenseConfig domain) {
        return new BranchExpenseConfigJpaEntity(
                domain.getBranchId(),
                domain.getAlertThresholdAmount(),
                domain.getEvidenceRequiredThreshold(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public BranchExpenseConfig toDomain() {
        return new BranchExpenseConfig(
                branchId,
                alertThresholdAmount,
                evidenceRequiredThreshold,
                createdAt,
                updatedAt
        );
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public BigDecimal getAlertThresholdAmount() {
        return alertThresholdAmount;
    }

    public void setAlertThresholdAmount(BigDecimal alertThresholdAmount) {
        this.alertThresholdAmount = alertThresholdAmount;
    }

    public BigDecimal getEvidenceRequiredThreshold() {
        return evidenceRequiredThreshold;
    }

    public void setEvidenceRequiredThreshold(BigDecimal evidenceRequiredThreshold) {
        this.evidenceRequiredThreshold = evidenceRequiredThreshold;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
