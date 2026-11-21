package com.mtms.hr.infrastructure.persistence;

import com.mtms.hr.domain.RateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-HR-02 / BR-HR-01 / TC-013
 * JPA entity for employee effective salary policies.
 */
@Entity
@Table(name = "salary_policies", schema = "mtms")
public class SalaryPolicyJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, length = 16)
    private RateType rateType;

    @Column(name = "rate_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal rateAmount;

    @Column(name = "standard_workdays", precision = 7, scale = 2)
    private BigDecimal standardWorkdays;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "paid_minutes_rounding_block")
    private Integer paidMinutesRoundingBlock;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SalaryPolicyJpaEntity() {
    }

    public SalaryPolicyJpaEntity(
            UUID id,
            UUID employeeId,
            UUID branchId,
            RateType rateType,
            BigDecimal rateAmount,
            BigDecimal standardWorkdays,
            Integer paidMinutesRoundingBlock,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes,
            UUID approvedBy,
            Instant createdAt
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.branchId = branchId;
        this.rateType = rateType;
        this.rateAmount = rateAmount;
        this.standardWorkdays = standardWorkdays;
        this.paidMinutesRoundingBlock = paidMinutesRoundingBlock;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.notes = notes;
        this.approvedBy = approvedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public RateType getRateType() {
        return rateType;
    }

    public void setRateType(RateType rateType) {
        this.rateType = rateType;
    }

    public BigDecimal getRateAmount() {
        return rateAmount;
    }

    public void setRateAmount(BigDecimal rateAmount) {
        this.rateAmount = rateAmount;
    }

    public BigDecimal getStandardWorkdays() {
        return standardWorkdays;
    }

    public void setStandardWorkdays(BigDecimal standardWorkdays) {
        this.standardWorkdays = standardWorkdays;
    }

    public Integer getPaidMinutesRoundingBlock() {
        return paidMinutesRoundingBlock;
    }

    public void setPaidMinutesRoundingBlock(Integer paidMinutesRoundingBlock) {
        this.paidMinutesRoundingBlock = paidMinutesRoundingBlock;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
