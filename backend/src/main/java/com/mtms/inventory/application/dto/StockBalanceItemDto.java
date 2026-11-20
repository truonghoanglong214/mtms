package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockBalanceItemDto(
        UUID id,
        UUID branchId,
        UUID itemId,
        String itemCode,
        String itemName,
        String unitCode,
        String unitName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal quantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal avgUnitCost,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal inventoryValue,
        Instant updatedAt
) {
}
