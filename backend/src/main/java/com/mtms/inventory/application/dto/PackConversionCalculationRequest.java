package com.mtms.inventory.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PackConversionCalculationRequest(
        @NotNull(message = "Item ID must not be null")
        UUID itemId,

        UUID supplierId,

        @NotNull(message = "Pack quantity must not be null")
        @DecimalMin(value = "0.001", inclusive = true, message = "Pack quantity must be strictly positive")
        BigDecimal packQuantity,

        @NotNull(message = "Business date must not be null")
        LocalDate businessDate
) {
}
