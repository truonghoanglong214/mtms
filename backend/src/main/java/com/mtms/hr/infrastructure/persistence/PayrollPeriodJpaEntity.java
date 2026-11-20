package com.mtms.hr.infrastructure.persistence;

import com.mtms.hr.domain.PayrollPeriodStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-HR-05 & P0-HR-06 / BR-HR-05
 * JPA entity mapping mtms.payroll_periods.
 */
@Entity
@Table(name = "payroll_periods", schema = "mtms")
public class PayrollPeriodJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "revision", nullable = false)
    private Integer revision;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PayrollPeriodStatus status;

    @Column(name = "total_gross_salary", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalGrossSalary;

    @Column(name = "total_base_salary", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalBaseSalary;

    @Column(name = "total_adjustments", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalAdjustments;

    @Column(name = "employee_count", nullable = false)
    private Integer employeeCount;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PayrollPeriodJpaEntity() {
    }

    public PayrollPeriodJpaEntity(
            UUID id,
            UUID branchId,
            LocalDate monthStart,
            Integer revision,
            Boolean isCurrent,
            PayrollPeriodStatus status,
            BigDecimal totalGrossSalary,
            BigDecimal totalBaseSalary,
            BigDecimal totalAdjustments,
            Integer employeeCount,
            String note,
            UUID approvedBy,
            Instant approvedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.branchId = branchId;
        this.monthStart = monthStart;
        this.revision = revision != null ? revision : 1;
        this.isCurrent = isCurrent != null ? isCurrent : true;
        this.status = status != null ? status : PayrollPeriodStatus.DRAFT;
        this.totalGrossSalary = totalGrossSalary != null ? totalGrossSalary : BigDecimal.ZERO;
        this.totalBaseSalary = totalBaseSalary != null ? totalBaseSalary : BigDecimal.ZERO;
        this.totalAdjustments = totalAdjustments != null ? totalAdjustments : BigDecimal.ZERO;
        this.employeeCount = employeeCount != null ? employeeCount : 0;
        this.note = note;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public LocalDate getMonthStart() {
        return monthStart;
    }

    public void setMonthStart(LocalDate monthStart) {
        this.monthStart = monthStart;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public PayrollPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(PayrollPeriodStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalGrossSalary() {
        return totalGrossSalary;
    }

    public void setTotalGrossSalary(BigDecimal totalGrossSalary) {
        this.totalGrossSalary = totalGrossSalary;
    }

    public BigDecimal getTotalBaseSalary() {
        return totalBaseSalary;
    }

    public void setTotalBaseSalary(BigDecimal totalBaseSalary) {
        this.totalBaseSalary = totalBaseSalary;
    }

    public BigDecimal getTotalAdjustments() {
        return totalAdjustments;
    }

    public void setTotalAdjustments(BigDecimal totalAdjustments) {
        this.totalAdjustments = totalAdjustments;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
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
