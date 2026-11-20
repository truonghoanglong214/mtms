package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.UUID;

public record TopValuedItemDto(
        UUID itemId,
        String itemCode,
        String itemName,
        String unitCode,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal quantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal avgUnitCost,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal inventoryValue
) {}
