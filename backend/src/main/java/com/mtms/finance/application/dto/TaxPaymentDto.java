package com.mtms.finance.application.dto;

import com.mtms.finance.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * DTO representing an individual tax payment installment.
 */
public record TaxPaymentDto(
        UUID id,
        UUID obligationId,
        UUID branchId,
        LocalDate paymentDate,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        UUID financialAccountId,
        String financialAccountName,
        UUID cashLedgerEntryId,
        String receiptNumber,
        String note,
        UUID evidenceFileId,
        UUID createdBy,
        Instant createdAt
) {}
