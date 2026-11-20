package com.mtms.finance.application.dto;

import com.mtms.finance.domain.ReceivableStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-FIN-05
 * Detailed view of receivable with receipts history.
 */
public record ReceivableDetailDto(
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
        Instant createdAt,
        List<ReceivableReceiptDto> receipts
) {
    public static ReceivableDetailDto of(
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
            Instant createdAt,
            List<ReceivableReceiptDto> receipts
    ) {
        BigDecimal remaining = (originalAmount != null && receivedAmount != null)
                ? originalAmount.subtract(receivedAmount)
                : BigDecimal.ZERO;
        return new ReceivableDetailDto(
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
                createdAt,
                receipts != null ? receipts : List.of()
        );
    }
}
