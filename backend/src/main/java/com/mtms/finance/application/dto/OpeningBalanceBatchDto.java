package com.mtms.finance.application.dto;

import com.mtms.finance.domain.OpeningBatchStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-FIN-08 / BR-FIN-08 / ADR-009
 * DTO representing an opening balance cutover batch.
 */
public record OpeningBalanceBatchDto(
        UUID id,
        UUID branchId,
        LocalDate cutoverDate,
        OpeningBatchStatus status,
        BigDecimal totalCashAmount,
        BigDecimal totalBankAmount,
        BigDecimal totalInventoryValuation,
        BigDecimal totalPayableAmount,
        BigDecimal totalReceivableAmount,
        BigDecimal totalTaxLiabilityAmount,
        BigDecimal netWorkingCapital,
        String summarySnapshot,
        String note,
        UUID evidenceFileId,
        String evidenceDownloadUrl,
        UUID createdById,
        String createdByName,
        UUID postedById,
        String postedByName,
        Instant postedAt,
        Instant createdAt,
        List<OpeningAccountBalanceDto> accountBalances
) {}
