package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.List;

public record StockBalanceResponse(
        List<StockBalanceItemDto> items,
        long totalItems,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalBranchValue
) {
}
