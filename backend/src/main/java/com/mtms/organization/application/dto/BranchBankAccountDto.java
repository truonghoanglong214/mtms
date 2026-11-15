package com.mtms.organization.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe public representation of a branch bank account.
 * Note: Raw account numbers and ciphertexts are never exposed in DTOs.
 */
public record BranchBankAccountDto(
        UUID id,
        UUID branchId,
        String bankName,
        String accountHolderName,
        String maskedAccountNumber,
        boolean isPrimary,
        String status,
        Instant createdAt
) {
}
