package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.UUID;

public record PackConversionCalculationResponse(
        UUID itemId,
        String itemCode,
        String itemName,
        UUID supplierId,
        String supplierName,
        String packName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal packQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal baseUnitsPerPack,
        String baseUnitCode,
        String baseUnitName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal calculatedBaseQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal conversionSnapshot,
        String formulaExplanation
) {
}
