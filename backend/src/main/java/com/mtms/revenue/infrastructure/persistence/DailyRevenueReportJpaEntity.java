package com.mtms.revenue.infrastructure.persistence;

import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.revenue.domain.DailyRevenueReport;
import com.mtms.revenue.domain.RevenueReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_revenue_reports", schema = "mtms")
public class DailyRevenueReportJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "revision", nullable = false)
    private int revision;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    @Column(name = "cash_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal cashAmount;

    @Column(name = "bank_transfer_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal bankTransferAmount;

    @Column(name = "delivery_app_net_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal deliveryAppNetAmount;

    @Column(name = "other_adjustment_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal otherAdjustmentAmount;

    @Column(name = "total_revenue", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalRevenue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private RevenueReportStatus status;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", insertable = false, updatable = false)
    private UserJpaEntity submittedByUser;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by", insertable = false, updatable = false)
    private UserJpaEntity confirmedByUser;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "adjustment_reason", columnDefinition = "TEXT")
    private String adjustmentReason;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DailyRevenueReportJpaEntity() {
    }

    public DailyRevenueReportJpaEntity(
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
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.businessDate = businessDate;
        this.revision = revision;
        this.isCurrent = isCurrent;
        this.cashAmount = cashAmount;
        this.bankTransferAmount = bankTransferAmount;
        this.deliveryAppNetAmount = deliveryAppNetAmount;
        this.otherAdjustmentAmount = otherAdjustmentAmount;
        this.totalRevenue = totalRevenue;
        this.status = status;
        this.submittedBy = submittedBy;
        this.submittedAt = submittedAt;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
        this.adjustmentReason = adjustmentReason;
        this.evidenceFileId = evidenceFileId;
        this.note = note;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static DailyRevenueReportJpaEntity fromDomain(DailyRevenueReport domain) {
        return new DailyRevenueReportJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getBusinessDate(),
                domain.getRevision(),
                domain.isCurrent(),
                domain.getCashAmount(),
                domain.getBankTransferAmount(),
                domain.getDeliveryAppNetAmount(),
                domain.getOtherAdjustmentAmount(),
                domain.getTotalRevenue(),
                domain.getStatus(),
                domain.getSubmittedBy(),
                domain.getSubmittedAt(),
                domain.getConfirmedBy(),
                domain.getConfirmedAt(),
                domain.getAdjustmentReason(),
                domain.getEvidenceFileId(),
                domain.getNote(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public DailyRevenueReport toDomain() {
        return new DailyRevenueReport(
                this.id,
                this.businessEntityId,
                this.branchId,
                this.businessDate,
                this.revision,
                this.isCurrent,
                this.cashAmount,
                this.bankTransferAmount,
                this.deliveryAppNetAmount,
                this.otherAdjustmentAmount,
                this.totalRevenue,
                this.status,
                this.submittedBy,
                this.submittedAt,
                this.confirmedBy,
                this.confirmedAt,
                this.adjustmentReason,
                this.evidenceFileId,
                this.note,
                this.createdAt,
                this.updatedAt
        );
    }

    public void updateFromDomain(DailyRevenueReport domain) {
        this.revision = domain.getRevision();
        this.isCurrent = domain.isCurrent();
        this.cashAmount = domain.getCashAmount();
        this.bankTransferAmount = domain.getBankTransferAmount();
        this.deliveryAppNetAmount = domain.getDeliveryAppNetAmount();
        this.otherAdjustmentAmount = domain.getOtherAdjustmentAmount();
        this.totalRevenue = domain.getTotalRevenue();
        this.status = domain.getStatus();
        this.submittedBy = domain.getSubmittedBy();
        this.submittedAt = domain.getSubmittedAt();
        this.confirmedBy = domain.getConfirmedBy();
        this.confirmedAt = domain.getConfirmedAt();
        this.adjustmentReason = domain.getAdjustmentReason();
        this.evidenceFileId = domain.getEvidenceFileId();
        this.note = domain.getNote();
        this.updatedAt = domain.getUpdatedAt();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public void setBusinessEntityId(UUID businessEntityId) {
        this.businessEntityId = businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    public BigDecimal getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(BigDecimal cashAmount) {
        this.cashAmount = cashAmount;
    }

    public BigDecimal getBankTransferAmount() {
        return bankTransferAmount;
    }

    public void setBankTransferAmount(BigDecimal bankTransferAmount) {
        this.bankTransferAmount = bankTransferAmount;
    }

    public BigDecimal getDeliveryAppNetAmount() {
        return deliveryAppNetAmount;
    }

    public void setDeliveryAppNetAmount(BigDecimal deliveryAppNetAmount) {
        this.deliveryAppNetAmount = deliveryAppNetAmount;
    }

    public BigDecimal getOtherAdjustmentAmount() {
        return otherAdjustmentAmount;
    }

    public void setOtherAdjustmentAmount(BigDecimal otherAdjustmentAmount) {
        this.otherAdjustmentAmount = otherAdjustmentAmount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public RevenueReportStatus getStatus() {
        return status;
    }

    public void setStatus(RevenueReportStatus status) {
        this.status = status;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UUID submittedBy) {
        this.submittedBy = submittedBy;
    }

    public UserJpaEntity getSubmittedByUser() {
        return submittedByUser;
    }

    public void setSubmittedByUser(UserJpaEntity submittedByUser) {
        this.submittedByUser = submittedByUser;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(UUID confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public UserJpaEntity getConfirmedByUser() {
        return confirmedByUser;
    }

    public void setConfirmedByUser(UserJpaEntity confirmedByUser) {
        this.confirmedByUser = confirmedByUser;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public void setAdjustmentReason(String adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public void setEvidenceFileId(UUID evidenceFileId) {
        this.evidenceFileId = evidenceFileId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
