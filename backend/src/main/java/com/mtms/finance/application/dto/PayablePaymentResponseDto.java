package com.mtms.finance.application.dto;

import com.mtms.finance.domain.PaymentMethod;
import com.mtms.inventory.domain.PayableStatus;
import com.mtms.inventory.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-05 / BR-FIN-02 / TC-009
 * Response DTO returned after settling a payable payment.
 */
public record PayablePaymentResponseDto(
        UUID paymentId,
        UUID payableId,
        UUID accountId,
        BigDecimal amount,
        LocalDate paidOn,
        PaymentMethod paymentMethod,
        String bankReference,
        UUID evidenceFileId,
        BigDecimal remainingDebt,
        PayableStatus payableStatus,
        PaymentStatus receiptPaymentStatus,
        UUID ledgerEntryId,
        Instant createdAt
) {
}
