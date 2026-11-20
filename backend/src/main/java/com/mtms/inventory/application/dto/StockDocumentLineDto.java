package com.mtms.inventory.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockDocumentLineDto(
        UUID id,
        UUID documentId,
        int lineNo,
        UUID itemId,
        String itemCode,
        String itemName,
        String unitName,
        UUID lotId,
        String lotCode,
        BigDecimal issueQuantity,
        BigDecimal bookedUnitCost,
        BigDecimal bookedTotal,
        String notes
) {}
