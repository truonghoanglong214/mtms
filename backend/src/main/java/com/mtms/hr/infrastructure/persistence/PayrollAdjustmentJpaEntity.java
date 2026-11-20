package com.mtms.hr.infrastructure.persistence;

import com.mtms.hr.domain.AdjustmentCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * P0-HR-05 / BR-HR-04
 * JPA entity mapping mtms.payroll_adjustments.
 */
@Entity
@Table(name = "payroll_adjustments", schema = "mtms")
public class PayrollAdjustmentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "payroll_line_id", nullable = false)
    private UUID payrollLineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_code", nullable = false, length = 50)
    private AdjustmentCode adjustmentCode;

    @Column(name = "signed_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal signedAmount;

    @Column(name = "explanation", columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PayrollAdjustmentJpaEntity() {
    }

    public PayrollAdjustmentJpaEntity(
            UUID id,
            UUID payrollLineId,
            AdjustmentCode adjustmentCode,
            BigDecimal signedAmount,
            String explanation,
            UUID approvedBy,
            Instant createdAt
    ) {
        this.id = id;
        this.payrollLineId = payrollLineId;
        this.adjustmentCode = adjustmentCode;
        this.signedAmount = signedAmount;
        this.explanation = explanation;
        this.approvedBy = approvedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPayrollLineId() {
        return payrollLineId;
    }

    public void setPayrollLineId(UUID payrollLineId) {
        this.payrollLineId = payrollLineId;
    }

    public AdjustmentCode getAdjustmentCode() {
        return adjustmentCode;
    }

    public void setAdjustmentCode(AdjustmentCode adjustmentCode) {
        this.adjustmentCode = adjustmentCode;
    }

    public BigDecimal getSignedAmount() {
        return signedAmount;
    }

    public void setSignedAmount(BigDecimal signedAmount) {
        this.signedAmount = signedAmount;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
