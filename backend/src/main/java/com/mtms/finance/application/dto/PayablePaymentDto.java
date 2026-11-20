package com.mtms.finance.application.dto;

import com.mtms.finance.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-05 / BR-FIN-02
 * DTO for debt installment payment.
 */
public record PayablePaymentDto(
        UUID id,
        UUID payableId,
        UUID accountId,
        String accountName,
        BigDecimal amount,
        LocalDate paidOn,
        PaymentMethod paymentMethod,
        String bankReference,
        UUID evidenceFileId,
        String status,
        UUID postedBy,
        Instant createdAt
) {
}
