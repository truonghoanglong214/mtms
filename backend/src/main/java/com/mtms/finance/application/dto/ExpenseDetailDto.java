package com.mtms.finance.application.dto;

import com.mtms.finance.domain.ExpenseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseDetailDto(
        UUID id,
        UUID branchId,
        String expenseCode,
        LocalDate expenseDate,
        UUID categoryId,
        String categoryCode,
        String categoryName,
        BigDecimal amount,
        boolean isPaid,
        UUID paymentAccountId,
        String paymentAccountName,
        UUID payableId,
        UUID evidenceFileId,
        String evidenceDownloadUrl,
        ExpenseStatus status,
        boolean reviewFlag,
        String reason,
        UUID createdBy,
        String createdByName,
        UUID confirmedBy,
        String confirmedByName,
        Instant confirmedAt,
        UUID voidedBy,
        String voidedByName,
        Instant voidedAt,
        String voidReason,
        Instant createdAt,
        CashLedgerEntryDto linkedCashLedgerEntry
) {
}
