package com.mtms.inventory.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateStockIssueLineCommand(
        @NotNull(message = "itemId must not be null")
        UUID itemId,

        @NotNull(message = "issueQuantity must not be null")
        @DecimalMin(value = "0.001", message = "issueQuantity must be at least 0.001")
        BigDecimal issueQuantity,

        UUID lotId,

        String notes
) {}
