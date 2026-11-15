package com.mtms.organization.infrastructure.persistence;

import com.mtms.organization.domain.BankAccountStatus;
import com.mtms.organization.domain.BranchBankAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "branch_bank_accounts", schema = "mtms")
public class BranchBankAccountJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "bank_name", nullable = false, length = 160)
    private String bankName;

    @Column(name = "account_holder_name", nullable = false, length = 160)
    private String accountHolderName;

    @Column(name = "account_number_ciphertext", nullable = false, columnDefinition = "TEXT")
    private String accountNumberCiphertext;

    @Column(name = "masked_account_number", nullable = false, length = 40)
    private String maskedAccountNumber;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private BankAccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BranchBankAccountJpaEntity() {
    }

    public BranchBankAccountJpaEntity(UUID id,
                                      UUID branchId,
                                      String bankName,
                                      String accountHolderName,
                                      String accountNumberCiphertext,
                                      String maskedAccountNumber,
                                      boolean isPrimary,
                                      BankAccountStatus status,
                                      Instant createdAt) {
        this.id = id;
        this.branchId = branchId;
        this.bankName = bankName;
        this.accountHolderName = accountHolderName;
        this.accountNumberCiphertext = accountNumberCiphertext;
        this.maskedAccountNumber = maskedAccountNumber;
        this.isPrimary = isPrimary;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static BranchBankAccountJpaEntity fromDomain(BranchBankAccount domain) {
        return new BranchBankAccountJpaEntity(
                domain.getId(),
                domain.getBranchId(),
                domain.getBankName(),
                domain.getAccountHolderName(),
                domain.getAccountNumberCiphertext(),
                domain.getMaskedAccountNumber(),
                domain.isPrimary(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }

    public BranchBankAccount toDomain() {
        return new BranchBankAccount(
                id,
                branchId,
                bankName,
                accountHolderName,
                accountNumberCiphertext,
                maskedAccountNumber,
                isPrimary,
                status,
                createdAt
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

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountNumberCiphertext() {
        return accountNumberCiphertext;
    }

    public void setAccountNumberCiphertext(String accountNumberCiphertext) {
        this.accountNumberCiphertext = accountNumberCiphertext;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public BankAccountStatus getStatus() {
        return status;
    }

    public void setStatus(BankAccountStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
