package com.mtms.finance.domain;

import com.mtms.common.decimal.MoneyUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-FIN-02 / TC-010
 * Internal cash movement record between two accounts of the same branch (Cash <-> Bank).
 * Net P&L impact = 0 VND.
 */
public class InternalTransfer {

    private final UUID id;
    private final UUID branchId;
    private final UUID fromAccountId;
    private final UUID toAccountId;
    private final BigDecimal amount;
    private final LocalDate businessDate;
    private String status;
    private final String note;
    private final UUID evidenceFileId;
    private final UUID createdBy;
    private final Instant createdAt;

    public InternalTransfer(
            UUID id,
            UUID branchId,
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            LocalDate businessDate,
            String status,
            String note,
            UUID evidenceFileId,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Transfer ID must not be null");
        this.branchId = Objects.requireNonNull(branchId, "Branch ID must not be null");
        this.fromAccountId = Objects.requireNonNull(fromAccountId, "From account ID must not be null");
        this.toAccountId = Objects.requireNonNull(toAccountId, "To account ID must not be null");
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransferAccountsException("Tài khoản nguồn và tài khoản đích phải khác nhau");
        }
        this.amount = MoneyUtils.roundMoney(Objects.requireNonNull(amount, "Amount must not be null"));
        if (this.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly positive");
        }
        this.businessDate = Objects.requireNonNull(businessDate, "Business date must not be null");
        this.status = status != null ? status : "COMPLETED";
        this.note = note;
        this.evidenceFileId = evidenceFileId;
        this.createdBy = Objects.requireNonNull(createdBy, "Created by must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalTransfer that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
