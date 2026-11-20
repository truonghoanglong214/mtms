package com.mtms.hr.infrastructure.persistence;

import com.mtms.hr.domain.TimesheetSourceType;
import com.mtms.hr.domain.TimesheetStatus;
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
 * P0-HR-03 & P0-HR-04 / BR-HR-02 / TC-017
 * JPA entity for employee timesheet entry and approval records.
 */
@Entity
@Table(name = "timesheet_entries", schema = "mtms")
public class TimesheetEntryJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "raw_minutes")
    private Integer rawMinutes;

    @Column(name = "approved_minutes", nullable = false)
    private Integer approvedMinutes;

    @Column(name = "payable_workdays", nullable = false, precision = 6, scale = 2)
    private BigDecimal payableWorkdays;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 24)
    private TimesheetSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TimesheetStatus status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approval_note", columnDefinition = "TEXT")
    private String approvalNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TimesheetEntryJpaEntity() {
    }

    public TimesheetEntryJpaEntity(
            UUID id,
            UUID employeeId,
            UUID branchId,
            LocalDate workDate,
            Integer rawMinutes,
            Integer approvedMinutes,
            BigDecimal payableWorkdays,
            TimesheetSourceType sourceType,
            TimesheetStatus status,
            String note,
            UUID approvedBy,
            Instant approvedAt,
            String approvalNote,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.branchId = branchId;
        this.workDate = workDate;
        this.rawMinutes = rawMinutes;
        this.approvedMinutes = approvedMinutes != null ? approvedMinutes : 0;
        this.payableWorkdays = payableWorkdays != null ? payableWorkdays : BigDecimal.ZERO;
        this.sourceType = sourceType != null ? sourceType : TimesheetSourceType.MANUAL;
        this.status = status != null ? status : TimesheetStatus.DRAFT;
        this.note = note;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.approvalNote = approvalNote;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
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

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public Integer getRawMinutes() {
        return rawMinutes;
    }

    public void setRawMinutes(Integer rawMinutes) {
        this.rawMinutes = rawMinutes;
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

    public TimesheetSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(TimesheetSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public TimesheetStatus getStatus() {
        return status;
    }

    public void setStatus(TimesheetStatus status) {
        this.status = status;
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

    public String getApprovalNote() {
        return approvalNote;
    }

    public void setApprovalNote(String approvalNote) {
        this.approvalNote = approvalNote;
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
