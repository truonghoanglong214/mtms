package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.PayableStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PayableDto(
        UUID id,
        UUID branchId,
        String counterpartyLabel,
        String sourceType,
        UUID sourceId,
        LocalDate incurredDate,
        LocalDate dueDate,
        BigDecimal originalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        PayableStatus status,
        Instant createdAt
) {
}
