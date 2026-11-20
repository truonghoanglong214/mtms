package com.mtms.finance.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BranchExpenseConfigDto(
        UUID branchId,
        BigDecimal alertThresholdAmount,
        BigDecimal evidenceRequiredThreshold,
        Instant createdAt,
        Instant updatedAt
) {
}
