package com.mtms.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseReceiptLineDto(
        UUID id,
        int lineNo,
        UUID itemId,
        String itemCode,
        String itemName,
        String baseUnitName,
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
