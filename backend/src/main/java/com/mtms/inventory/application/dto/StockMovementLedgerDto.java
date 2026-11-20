package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mtms.inventory.domain.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StockMovementLedgerDto(
        UUID id,
        UUID branchId,
        UUID itemId,
        String itemCode,
        String itemName,
        String unitCode,
        String unitName,
        LocalDate businessDate,
        StockMovementType movementType,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal signedQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal signedValue,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal bookedAvgCost,
        String sourceType,
        UUID sourceId,
        String sourceRef,
        String createdByName,
        Instant createdAt
) {}
