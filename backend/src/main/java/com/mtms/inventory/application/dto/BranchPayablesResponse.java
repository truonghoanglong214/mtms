package com.mtms.inventory.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record BranchPayablesResponse(
        List<PayableDto> items,
        long totalItems,
        BigDecimal totalOutstandingDebt
) {
}
