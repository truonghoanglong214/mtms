package com.mtms.finance.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009
 * JPA entity representing the opening balance of an individual financial account within an opening batch.
 */
@Entity
@Table(name = "opening_account_balances", schema = "mtms")
public class OpeningAccountBalanceJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "opening_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "cash_ledger_entry_id")
    private UUID cashLedgerEntryId;

    public OpeningAccountBalanceJpaEntity() {
    }

    public OpeningAccountBalanceJpaEntity(
            UUID id,
            UUID batchId,
            UUID accountId,
            BigDecimal openingBalance,
            UUID cashLedgerEntryId
    ) {
        this.id = id;
        this.batchId = batchId;
        this.accountId = accountId;
        this.openingBalance = openingBalance;
        this.cashLedgerEntryId = cashLedgerEntryId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public UUID getCashLedgerEntryId() {
        return cashLedgerEntryId;
    }

    public void setCashLedgerEntryId(UUID cashLedgerEntryId) {
        this.cashLedgerEntryId = cashLedgerEntryId;
    }
}
