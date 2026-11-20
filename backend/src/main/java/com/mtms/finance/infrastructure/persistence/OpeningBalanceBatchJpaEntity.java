package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.OpeningBatchStatus;
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
 * P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009
 * JPA entity representing the opening balance batch for a branch.
 */
@Entity
@Table(name = "opening_balance_batches", schema = "mtms")
public class OpeningBalanceBatchJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "branch_id", nullable = false, unique = true)
    private UUID branchId;

    @Column(name = "cutover_date", nullable = false)
    private LocalDate cutoverDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OpeningBatchStatus status;

    @Column(name = "total_cash_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalCashAmount;

    @Column(name = "total_bank_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalBankAmount;

    @Column(name = "total_inventory_valuation", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalInventoryValuation;

    @Column(name = "total_payable_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalPayableAmount;

    @Column(name = "total_receivable_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalReceivableAmount;

    @Column(name = "total_tax_liability_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalTaxLiabilityAmount;

    @Column(name = "net_working_capital", nullable = false, precision = 20, scale = 2)
    private BigDecimal netWorkingCapital;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_snapshot", nullable = false)
    private String summarySnapshot;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "posted_by")
    private UUID postedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public OpeningBalanceBatchJpaEntity() {
    }

    public OpeningBalanceBatchJpaEntity(
            UUID id,
            UUID branchId,
            LocalDate cutoverDate,
            OpeningBatchStatus status,
            BigDecimal totalCashAmount,
            BigDecimal totalBankAmount,
            BigDecimal totalInventoryValuation,
            BigDecimal totalPayableAmount,
            BigDecimal totalReceivableAmount,
            BigDecimal totalTaxLiabilityAmount,
            BigDecimal netWorkingCapital,
            String summarySnapshot,
            String note,
            UUID evidenceFileId,
            UUID createdBy,
            UUID postedBy,
            Instant postedAt,
            Instant createdAt
    ) {
        this.id = id;
        this.branchId = branchId;
        this.cutoverDate = cutoverDate;
        this.status = status;
        this.totalCashAmount = totalCashAmount;
        this.totalBankAmount = totalBankAmount;
        this.totalInventoryValuation = totalInventoryValuation;
        this.totalPayableAmount = totalPayableAmount;
        this.totalReceivableAmount = totalReceivableAmount;
        this.totalTaxLiabilityAmount = totalTaxLiabilityAmount;
        this.netWorkingCapital = netWorkingCapital;
        this.summarySnapshot = summarySnapshot != null ? summarySnapshot : "{}";
        this.note = note;
        this.evidenceFileId = evidenceFileId;
        this.createdBy = createdBy;
        this.postedBy = postedBy;
        this.postedAt = postedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
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

    public LocalDate getCutoverDate() {
        return cutoverDate;
    }

    public void setCutoverDate(LocalDate cutoverDate) {
        this.cutoverDate = cutoverDate;
    }

    public OpeningBatchStatus getStatus() {
        return status;
    }

    public void setStatus(OpeningBatchStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalCashAmount() {
        return totalCashAmount;
    }

    public void setTotalCashAmount(BigDecimal totalCashAmount) {
        this.totalCashAmount = totalCashAmount;
    }

    public BigDecimal getTotalBankAmount() {
        return totalBankAmount;
    }

    public void setTotalBankAmount(BigDecimal totalBankAmount) {
        this.totalBankAmount = totalBankAmount;
    }

    public BigDecimal getTotalInventoryValuation() {
        return totalInventoryValuation;
    }

    public void setTotalInventoryValuation(BigDecimal totalInventoryValuation) {
        this.totalInventoryValuation = totalInventoryValuation;
    }

    public BigDecimal getTotalPayableAmount() {
        return totalPayableAmount;
    }

    public void setTotalPayableAmount(BigDecimal totalPayableAmount) {
        this.totalPayableAmount = totalPayableAmount;
    }

    public BigDecimal getTotalReceivableAmount() {
        return totalReceivableAmount;
    }

    public void setTotalReceivableAmount(BigDecimal totalReceivableAmount) {
        this.totalReceivableAmount = totalReceivableAmount;
    }

    public BigDecimal getTotalTaxLiabilityAmount() {
        return totalTaxLiabilityAmount;
    }

    public void setTotalTaxLiabilityAmount(BigDecimal totalTaxLiabilityAmount) {
        this.totalTaxLiabilityAmount = totalTaxLiabilityAmount;
    }

    public BigDecimal getNetWorkingCapital() {
        return netWorkingCapital;
    }

    public void setNetWorkingCapital(BigDecimal netWorkingCapital) {
        this.netWorkingCapital = netWorkingCapital;
    }

    public String getSummarySnapshot() {
        return summarySnapshot;
    }

    public void setSummarySnapshot(String summarySnapshot) {
        this.summarySnapshot = summarySnapshot;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public void setEvidenceFileId(UUID evidenceFileId) {
        this.evidenceFileId = evidenceFileId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(UUID postedBy) {
        this.postedBy = postedBy;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
