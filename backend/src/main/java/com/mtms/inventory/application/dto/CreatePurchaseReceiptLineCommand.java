package com.mtms.inventory.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePurchaseReceiptLineCommand(
        @NotNull(message = "itemId must not be null")
        UUID itemId,
        String packName,
        BigDecimal declaredPackQuantity,
        BigDecimal conversionSnapshot,
        BigDecimal receivedBaseQuantity,
        BigDecimal baseUnitCost,
        BigDecimal lineTotal,
        String lotCode,
        LocalDate expiresOn
) {
}
