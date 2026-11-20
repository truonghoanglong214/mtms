package com.mtms.inventory.domain;

import java.math.BigDecimal;

public record PackConversionCalculationResult(
        BigDecimal calculatedBaseQuantity,
        BigDecimal conversionSnapshot,
        String formulaExplanation
) {
}
