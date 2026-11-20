package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.CashLedgerEntry;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.LedgerStatus;
import com.mtms.finance.domain.SourceType;
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

@Entity
@Table(name = "cash_ledger_entries", schema = "mtms")
public class CashLedgerEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "signed_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal signedAmount;

    @Column(name = "running_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal runningBalance;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 48)
    private SourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "source_leg", nullable = false, length = 24)
    private String sourceLeg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private LedgerStatus status;

    @Column(name = "reversal_of_id")
    private UUID reversalOfId;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    public CashLedgerEntryJpaEntity() {
    }

    public CashLedgerEntryJpaEntity(
            UUID id,
            UUID accountId,
            UUID branchId,
            LocalDate businessDate,
            EntryType entryType,
            BigDecimal amount,
            BigDecimal signedAmount,
            BigDecimal runningBalance,
            UUID categoryId,
            SourceType sourceType,
            UUID sourceId,
            String sourceLeg,
            LedgerStatus status,
            UUID reversalOfId,
            String memo,
            UUID evidenceFileId,
            UUID createdBy,
            Instant postedAt
    ) {
        this.id = id;
        this.accountId = accountId;
        this.branchId = branchId;
        this.businessDate = businessDate;
        this.entryType = entryType;
        this.amount = amount;
        this.signedAmount = signedAmount;
        this.runningBalance = runningBalance;
        this.categoryId = categoryId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceLeg = sourceLeg != null ? sourceLeg : "MAIN";
        this.status = status != null ? status : LedgerStatus.POSTED;
        this.reversalOfId = reversalOfId;
        this.memo = memo;
        this.evidenceFileId = evidenceFileId;
        this.createdBy = createdBy;
        this.postedAt = postedAt != null ? postedAt : Instant.now();
    }

    public static CashLedgerEntryJpaEntity fromDomain(CashLedgerEntry domain) {
        return new CashLedgerEntryJpaEntity(
                domain.getId(),
                domain.getAccountId(),
                domain.getBranchId(),
                domain.getBusinessDate(),
                domain.getEntryType(),
                domain.getAmount(),
                domain.getSignedAmount(),
                domain.getRunningBalance(),
                domain.getCategoryId(),
                domain.getSourceType(),
                domain.getSourceId(),
                domain.getSourceLeg(),
                domain.getStatus(),
                domain.getReversalOfId(),
                domain.getMemo(),
                domain.getEvidenceFileId(),
                domain.getCreatedBy(),
                domain.getPostedAt()
        );
    }

    public CashLedgerEntry toDomain() {
        return new CashLedgerEntry(
                this.id,
                this.accountId,
                this.branchId,
                this.businessDate,
                this.entryType,
                this.amount,
                this.signedAmount,
                this.runningBalance,
                this.categoryId,
                this.sourceType,
                this.sourceId,
                this.sourceLeg,
                this.status,
                this.reversalOfId,
                this.memo,
                this.evidenceFileId,
                this.createdBy,
                this.postedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
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

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getSignedAmount() {
        return signedAmount;
    }

    public void setSignedAmount(BigDecimal signedAmount) {
        this.signedAmount = signedAmount;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceLeg() {
        return sourceLeg;
    }

    public void setSourceLeg(String sourceLeg) {
        this.sourceLeg = sourceLeg;
    }

    public LedgerStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerStatus status) {
        this.status = status;
    }

    public UUID getReversalOfId() {
        return reversalOfId;
    }

    public void setReversalOfId(UUID reversalOfId) {
        this.reversalOfId = reversalOfId;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
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

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }
}
