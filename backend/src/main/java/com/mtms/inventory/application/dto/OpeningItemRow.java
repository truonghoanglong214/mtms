package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpeningItemRow(
        @NotBlank(message = "Item code cannot be blank")
        String itemCode,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal quantity,

        @NotNull(message = "Unit cost is required")
        @DecimalMin(value = "0.0", message = "Unit cost must be non-negative")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal unitCost,

        String lotCode,

        LocalDate expiresOn
) {
}
