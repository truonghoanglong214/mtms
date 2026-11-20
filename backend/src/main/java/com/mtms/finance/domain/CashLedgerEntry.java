package com.mtms.finance.domain;

import com.mtms.common.decimal.MoneyUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-FIN-02 / BR-FIN-09 / TC-011
 * Immutable cash/bank ledger entry capturing a financial inflow or outflow.
 * Source uniqueness constraint: UNIQUE (account_id, source_type, source_id, source_leg).
 */
public class CashLedgerEntry {

    private final UUID id;
    private final UUID accountId;
    private final UUID branchId;
    private final LocalDate businessDate;
    private final EntryType entryType;
    private final BigDecimal amount;
    private final BigDecimal signedAmount;
    private final BigDecimal runningBalance;
    private final UUID categoryId;
    private final SourceType sourceType;
    private final UUID sourceId;
    private final String sourceLeg;
    private LedgerStatus status;
    private final UUID reversalOfId;
    private final String memo;
    private final UUID evidenceFileId;
    private final UUID createdBy;
    private final Instant postedAt;

    public CashLedgerEntry(
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
        this.id = Objects.requireNonNull(id, "Entry ID must not be null");
        this.accountId = Objects.requireNonNull(accountId, "Account ID must not be null");
        this.branchId = Objects.requireNonNull(branchId, "Branch ID must not be null");
        this.businessDate = Objects.requireNonNull(businessDate, "Business date must not be null");
        this.entryType = Objects.requireNonNull(entryType, "Entry type must not be null");
        this.amount = MoneyUtils.roundMoney(Objects.requireNonNull(amount, "Amount must not be null"));
        if (this.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be strictly positive");
        }
        this.signedAmount = signedAmount != null ? MoneyUtils.roundMoney(signedAmount) :
                (entryType == EntryType.INFLOW ? this.amount : this.amount.negate());
        this.runningBalance = MoneyUtils.roundMoney(Objects.requireNonNull(runningBalance, "Running balance must not be null"));
        this.categoryId = Objects.requireNonNull(categoryId, "Category ID must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "Source type must not be null");
        this.sourceId = Objects.requireNonNull(sourceId, "Source ID must not be null");
        this.sourceLeg = sourceLeg != null ? sourceLeg : "MAIN";
        this.status = status != null ? status : LedgerStatus.POSTED;
        this.reversalOfId = reversalOfId;
        this.memo = memo;
        this.evidenceFileId = evidenceFileId;
        this.createdBy = Objects.requireNonNull(createdBy, "Created by must not be null");
        this.postedAt = postedAt != null ? postedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getSignedAmount() {
        return signedAmount;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getSourceLeg() {
        return sourceLeg;
    }

    public LedgerStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerStatus status) {
        this.status = Objects.requireNonNull(status, "Status must not be null");
    }

    public UUID getReversalOfId() {
        return reversalOfId;
    }

    public String getMemo() {
        return memo;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CashLedgerEntry that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
