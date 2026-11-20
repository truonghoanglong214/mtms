package com.mtms.finance.application.dto;

import com.mtms.inventory.domain.PayableStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-FIN-05 / BR-FIN-02
 * Detailed view of payable with installment payment history.
 */
public record PayableDetailDto(
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
        Instant createdAt,
        List<PayablePaymentDto> payments
) {
    public static PayableDetailDto of(
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
            Instant createdAt,
            List<PayablePaymentDto> payments
    ) {
        BigDecimal remaining = (originalAmount != null && paidAmount != null)
                ? originalAmount.subtract(paidAmount)
                : BigDecimal.ZERO;
        return new PayableDetailDto(
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
                createdAt,
                payments != null ? payments : List.of()
        );
    }
}
