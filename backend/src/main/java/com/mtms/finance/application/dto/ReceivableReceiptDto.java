package com.mtms.finance.application.dto;

import com.mtms.finance.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-05
 * DTO for money collected against a receivable.
 */
public record ReceivableReceiptDto(
        UUID id,
        UUID receivableId,
        UUID accountId,
        String accountName,
        BigDecimal amount,
        LocalDate receivedOn,
        PaymentMethod paymentMethod,
        String bankReference,
        UUID evidenceFileId,
        String status,
        UUID postedBy,
        Instant createdAt
) {
}
