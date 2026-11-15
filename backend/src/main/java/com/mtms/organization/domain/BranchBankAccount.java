package com.mtms.organization.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-ORG-07 / BR-ORG-03 / TC-003
 * Domain entity representing a dedicated branch bank account.
 * Invariant: Every branch must have at least one active bank account to pass go-live readiness.
 */
public class BranchBankAccount {

    private final UUID id;
    private final UUID branchId;
    private final String bankName;
    private final String accountHolderName;
    private final String accountNumberCiphertext;
    private final String maskedAccountNumber;
    private final boolean isPrimary;
    private final BankAccountStatus status;
    private final Instant createdAt;

    public BranchBankAccount(UUID id,
                             UUID branchId,
                             String bankName,
                             String accountHolderName,
                             String accountNumberCiphertext,
                             String maskedAccountNumber,
                             boolean isPrimary,
                             BankAccountStatus status,
                             Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Account ID must not be null");
        this.branchId = Objects.requireNonNull(branchId, "Branch ID must not be null");
        this.bankName = validateString(bankName, 160, "Bank name");
        this.accountHolderName = validateString(accountHolderName, 160, "Account holder name");
        this.accountNumberCiphertext = Objects.requireNonNull(accountNumberCiphertext, "Ciphertext must not be null");
        this.maskedAccountNumber = validateString(maskedAccountNumber, 40, "Masked account number");
        this.isPrimary = isPrimary;
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
    }

    private static String validateString(String val, int maxLen, String fieldName) {
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (val.trim().length() > maxLen) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + maxLen + " characters");
        }
        return val.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public String getAccountNumberCiphertext() {
        return accountNumberCiphertext;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public BankAccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
