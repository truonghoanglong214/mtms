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
import java.util.UUID;

/**
 * P0-HR-05 & P0-HR-06 / BR-HR-04 / BR-HR-10
 * JPA entity mapping mtms.payroll_lines.
 */
@Entity
@Table(name = "payroll_lines", schema = "mtms")
public class PayrollLineJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "payroll_period_id", nullable = false)
    private UUID payrollPeriodId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "expense_branch_id", nullable = false)
    private UUID expenseBranchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, length = 16)
    private RateType rateType;

    @Column(name = "approved_minutes", nullable = false)
    private Integer approvedMinutes;

    @Column(name = "payable_workdays", nullable = false, precision = 7, scale = 2)
    private BigDecimal payableWorkdays;

    @Column(name = "base_salary", nullable = false, precision = 20, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "adjustments_total", nullable = false, precision = 20, scale = 2)
    private BigDecimal adjustmentsTotal;

    @Column(name = "gross_salary", nullable = false, precision = 20, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "paid_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal paidAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_snapshot", nullable = false)
    private String policySnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calculation_snapshot", nullable = false)
    private String calculationSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PayrollLineJpaEntity() {
    }

    public PayrollLineJpaEntity(
            UUID id,
            UUID payrollPeriodId,
            UUID employeeId,
            UUID expenseBranchId,
            RateType rateType,
            Integer approvedMinutes,
            BigDecimal payableWorkdays,
            BigDecimal baseSalary,
            BigDecimal adjustmentsTotal,
            BigDecimal grossSalary,
            BigDecimal paidAmount,
            String policySnapshot,
            String calculationSnapshot,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.payrollPeriodId = payrollPeriodId;
        this.employeeId = employeeId;
        this.expenseBranchId = expenseBranchId;
        this.rateType = rateType;
        this.approvedMinutes = approvedMinutes != null ? approvedMinutes : 0;
        this.payableWorkdays = payableWorkdays != null ? payableWorkdays : BigDecimal.ZERO;
        this.baseSalary = baseSalary != null ? baseSalary : BigDecimal.ZERO;
        this.adjustmentsTotal = adjustmentsTotal != null ? adjustmentsTotal : BigDecimal.ZERO;
        this.grossSalary = grossSalary != null ? grossSalary : BigDecimal.ZERO;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.policySnapshot = policySnapshot != null ? policySnapshot : "{}";
        this.calculationSnapshot = calculationSnapshot != null ? calculationSnapshot : "{}";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPayrollPeriodId() {
        return payrollPeriodId;
    }

    public void setPayrollPeriodId(UUID payrollPeriodId) {
        this.payrollPeriodId = payrollPeriodId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public UUID getExpenseBranchId() {
        return expenseBranchId;
    }

    public void setExpenseBranchId(UUID expenseBranchId) {
        this.expenseBranchId = expenseBranchId;
    }

    public RateType getRateType() {
        return rateType;
    }

    public void setRateType(RateType rateType) {
        this.rateType = rateType;
    }

    public Integer getApprovedMinutes() {
        return approvedMinutes;
    }

    public void setApprovedMinutes(Integer approvedMinutes) {
        this.approvedMinutes = approvedMinutes;
    }

    public BigDecimal getPayableWorkdays() {
        return payableWorkdays;
    }

    public void setPayableWorkdays(BigDecimal payableWorkdays) {
        this.payableWorkdays = payableWorkdays;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getAdjustmentsTotal() {
        return adjustmentsTotal;
    }

    public void setAdjustmentsTotal(BigDecimal adjustmentsTotal) {
        this.adjustmentsTotal = adjustmentsTotal;
    }

    public BigDecimal getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(BigDecimal grossSalary) {
        this.grossSalary = grossSalary;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPolicySnapshot() {
        return policySnapshot;
    }

    public void setPolicySnapshot(String policySnapshot) {
        this.policySnapshot = policySnapshot;
    }

    public String getCalculationSnapshot() {
        return calculationSnapshot;
    }

    public void setCalculationSnapshot(String calculationSnapshot) {
        this.calculationSnapshot = calculationSnapshot;
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
