package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.ReconciliationDisposition;
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
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * JPA entity representing a cash or bank account reconciliation snapshot.
 */
@Entity
@Table(name = "cash_reconciliations", schema = "mtms")
public class CashReconciliationJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "recorded_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal recordedBalance;

    @Column(name = "observed_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal observedBalance;

    @Column(name = "variance_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal varianceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 24)
    private ReconciliationDisposition disposition;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "adjustment_ledger_entry_id")
    private UUID adjustmentLedgerEntryId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CashReconciliationJpaEntity() {
    }

    public CashReconciliationJpaEntity(
            UUID id,
            UUID branchId,
            UUID accountId,
            LocalDate reconciliationDate,
            BigDecimal recordedBalance,
            BigDecimal observedBalance,
            BigDecimal varianceAmount,
            ReconciliationDisposition disposition,
            String note,
            UUID evidenceFileId,
            UUID adjustmentLedgerEntryId,
            UUID createdBy,
            UUID confirmedBy,
            Instant confirmedAt,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.branchId = branchId;
        this.accountId = accountId;
        this.reconciliationDate = reconciliationDate;
        this.recordedBalance = recordedBalance;
        this.observedBalance = observedBalance;
        this.varianceAmount = varianceAmount;
        this.disposition = disposition != null ? disposition : ReconciliationDisposition.OPEN;
        this.note = note;
        this.evidenceFileId = evidenceFileId;
        this.adjustmentLedgerEntryId = adjustmentLedgerEntryId;
        this.createdBy = createdBy;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
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

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public BigDecimal getRecordedBalance() {
        return recordedBalance;
    }

    public void setRecordedBalance(BigDecimal recordedBalance) {
        this.recordedBalance = recordedBalance;
    }

    public BigDecimal getObservedBalance() {
        return observedBalance;
    }

    public void setObservedBalance(BigDecimal observedBalance) {
        this.observedBalance = observedBalance;
    }

    public BigDecimal getVarianceAmount() {
        return varianceAmount;
    }

    public void setVarianceAmount(BigDecimal varianceAmount) {
        this.varianceAmount = varianceAmount;
    }

    public ReconciliationDisposition getDisposition() {
        return disposition;
    }

    public void setDisposition(ReconciliationDisposition disposition) {
        this.disposition = disposition;
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

    public UUID getAdjustmentLedgerEntryId() {
        return adjustmentLedgerEntryId;
    }

    public void setAdjustmentLedgerEntryId(UUID adjustmentLedgerEntryId) {
        this.adjustmentLedgerEntryId = adjustmentLedgerEntryId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(UUID confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
