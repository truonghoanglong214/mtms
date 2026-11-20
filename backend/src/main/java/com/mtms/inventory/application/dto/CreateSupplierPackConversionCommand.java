package com.mtms.inventory.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateSupplierPackConversionCommand(
        @NotNull(message = "Supplier ID must not be null")
        UUID supplierId,

        @NotBlank(message = "Pack name must not be blank")
        @Size(max = 100, message = "Pack name must be at most 100 characters")
        String packName,

        @NotNull(message = "Base units per pack must not be null")
        @DecimalMin(value = "0.001", inclusive = true, message = "Base units per pack must be greater than zero")
        BigDecimal baseUnitsPerPack,

        @NotNull(message = "Effective from date must not be null")
        LocalDate effectiveFrom,

        LocalDate effectiveTo
) {
}
