package com.mtms.finance.application.dto;

import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.ReconciliationDisposition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * Data transfer object for cash & bank reconciliation records.
 */
public record CashReconciliationDto(
        UUID id,
        UUID branchId,
        UUID accountId,
        String accountName,
        AccountKind accountKind,
        String currency,
        LocalDate reconciliationDate,
        BigDecimal recordedBalance,
        BigDecimal observedBalance,
        BigDecimal varianceAmount,
        ReconciliationDisposition disposition,
        String note,
        UUID evidenceFileId,
        String evidenceDownloadUrl,
        UUID adjustmentLedgerEntryId,
        UUID createdById,
        String createdByName,
        UUID confirmedById,
        String confirmedByName,
        Instant confirmedAt,
        Instant createdAt
) {}
