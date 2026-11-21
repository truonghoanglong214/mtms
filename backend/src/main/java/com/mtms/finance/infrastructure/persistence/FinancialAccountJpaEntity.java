package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.FinancialAccount;
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
import java.util.UUID;

@Entity
@Table(name = "financial_accounts", schema = "mtms")
public class FinancialAccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "bank_account_id")
    private UUID bankAccountId;

    @Column(name = "account_name", nullable = false, length = 160)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_kind", nullable = false, length = 16)
    private AccountKind accountKind;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "current_balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal currentBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FinancialAccountJpaEntity() {
    }

    public FinancialAccountJpaEntity(
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
        this.id = id;
        this.branchId = branchId;
        this.bankAccountId = bankAccountId;
        this.accountName = accountName;
        this.accountKind = accountKind;
        this.currency = currency != null ? currency : "VND";
        this.currentBalance = currentBalance != null ? currentBalance : BigDecimal.ZERO;
        this.status = status != null ? status : AccountStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static FinancialAccountJpaEntity fromDomain(FinancialAccount domain) {
        return new FinancialAccountJpaEntity(
                domain.getId(),
                domain.getBranchId(),
                domain.getBankAccountId(),
                domain.getAccountName(),
                domain.getAccountKind(),
                domain.getCurrency(),
                domain.getCurrentBalance(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }

    public FinancialAccount toDomain() {
        return new FinancialAccount(
                this.id,
                this.branchId,
                this.bankAccountId,
                this.accountName,
                this.accountKind,
                this.currency,
                this.currentBalance,
                this.status,
                this.createdAt
        );
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

    public UUID getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(UUID bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public AccountKind getAccountKind() {
        return accountKind;
    }

    public void setAccountKind(AccountKind accountKind) {
        this.accountKind = accountKind;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
