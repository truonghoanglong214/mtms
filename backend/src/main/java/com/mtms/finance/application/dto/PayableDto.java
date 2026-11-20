package com.mtms.finance.application.dto;

import com.mtms.inventory.domain.PayableStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-05 / BR-FIN-02
 * DTO for branch payables ledger.
 */
public record PayableDto(
        UUID id,
        UUID businessEntityId,
        UUID branchId,
        String counterpartyLabel,
        String sourceType,
        UUID sourceId,
        LocalDate incurredDate,
        LocalDate dueDate,
        BigDecimal originalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingDebt,
        BigDecimal remainingAmount,
        PayableStatus status,
        Instant createdAt
) {
    public static PayableDto of(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String counterpartyLabel,
            String sourceType,
            UUID sourceId,
            LocalDate incurredDate,
            LocalDate dueDate,
            BigDecimal originalAmount,
            BigDecimal paidAmount,
            PayableStatus status,
            Instant createdAt
    ) {
        BigDecimal remaining = (originalAmount != null && paidAmount != null)
                ? originalAmount.subtract(paidAmount)
                : BigDecimal.ZERO;
        return new PayableDto(
                id,
                businessEntityId,
                branchId,
                counterpartyLabel,
                sourceType,
                sourceId,
                incurredDate,
                dueDate,
                originalAmount,
                paidAmount,
                remaining,
                remaining,
                status,
                createdAt
        );
    }
}
