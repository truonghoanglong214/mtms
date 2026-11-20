package com.mtms.finance.domain;

import com.mtms.common.decimal.MoneyUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-FIN-02 / BR-FIN-09
 * Financial Account aggregate root representing a branch's cash drawer or bank account.
 * Maintains current balance updated with pessimistic row locks.
 */
public class FinancialAccount {

    private final UUID id;
    private final UUID branchId;
    private final UUID bankAccountId;
    private String accountName;
    private final AccountKind accountKind;
    private final String currency;
    private BigDecimal currentBalance;
    private AccountStatus status;
    private final Instant createdAt;

    public FinancialAccount(
            UUID id,
            UUID branchId,
            UUID bankAccountId,
            String accountName,
            AccountKind accountKind,
            String currency,
            BigDecimal currentBalance,
            AccountStatus status,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Account ID must not be null");
        this.branchId = Objects.requireNonNull(branchId, "Branch ID must not be null");
        this.accountKind = Objects.requireNonNull(accountKind, "Account kind must not be null");
        if (accountKind == AccountKind.CASH && bankAccountId != null) {
            throw new IllegalArgumentException("Cash account must not have bank account reference");
        }
        this.bankAccountId = bankAccountId;
        this.accountName = Objects.requireNonNull(accountName, "Account name must not be null").trim();
        this.currency = currency != null ? currency : "VND";
        this.currentBalance = currentBalance != null ? MoneyUtils.roundMoney(currentBalance) : MoneyUtils.ZERO_MONEY;
        this.status = status != null ? status : AccountStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static FinancialAccount createDefaultCashAccount(UUID id, UUID branchId) {
        return new FinancialAccount(
                id,
                branchId,
                null,
                "Quỹ tiền mặt chi nhánh",
                AccountKind.CASH,
                "VND",
                MoneyUtils.ZERO_MONEY,
                AccountStatus.ACTIVE,
                Instant.now()
        );
    }

    public static FinancialAccount createBankAccount(
            UUID id,
            UUID branchId,
            UUID bankAccountId,
            String accountName
    ) {
        return new FinancialAccount(
                id,
                branchId,
                Objects.requireNonNull(bankAccountId, "Bank account ID must not be null for BANK kind"),
                accountName,
                AccountKind.BANK,
                "VND",
                MoneyUtils.ZERO_MONEY,
                AccountStatus.ACTIVE,
                Instant.now()
        );
    }

    public synchronized void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be strictly positive");
        }
        this.currentBalance = MoneyUtils.roundMoney(this.currentBalance.add(amount));
    }

    public synchronized void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be strictly positive");
        }
        BigDecimal roundedAmount = MoneyUtils.roundMoney(amount);
        if (this.currentBalance.compareTo(roundedAmount) < 0) {
            throw new InsufficientAccountBalanceException(this.id, roundedAmount, this.currentBalance);
        }
        this.currentBalance = MoneyUtils.roundMoney(this.currentBalance.subtract(roundedAmount));
    }

    public UUID getId() {
        return id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getBankAccountId() {
        return bankAccountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = Objects.requireNonNull(accountName, "Account name must not be null").trim();
    }

    public AccountKind getAccountKind() {
        return accountKind;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = Objects.requireNonNull(status, "Status must not be null");
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialAccount that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
