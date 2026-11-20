package com.mtms.finance.application.dto;

import com.mtms.finance.domain.ReceivableStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-05
 * DTO for branch receivables ledger.
 */
public record ReceivableDto(
        UUID id,
        UUID businessEntityId,
        UUID branchId,
        String counterpartyLabel,
        String sourceType,
        UUID sourceId,
        LocalDate incurredDate,
        LocalDate dueDate,
        BigDecimal originalAmount,
        BigDecimal receivedAmount,
        BigDecimal remainingAmount,
        ReceivableStatus status,
        String notes,
        UUID createdBy,
        Instant createdAt
) {
    public static ReceivableDto of(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String counterpartyLabel,
            String sourceType,
            UUID sourceId,
            LocalDate incurredDate,
            LocalDate dueDate,
            BigDecimal originalAmount,
            BigDecimal receivedAmount,
            ReceivableStatus status,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        BigDecimal remaining = (originalAmount != null && receivedAmount != null)
                ? originalAmount.subtract(receivedAmount)
                : BigDecimal.ZERO;
        return new ReceivableDto(
                id,
                businessEntityId,
                branchId,
                counterpartyLabel,
                sourceType,
                sourceId,
                incurredDate,
                dueDate,
                originalAmount,
                receivedAmount,
                remaining,
                status,
                notes,
                createdBy,
                createdAt
        );
    }
}
