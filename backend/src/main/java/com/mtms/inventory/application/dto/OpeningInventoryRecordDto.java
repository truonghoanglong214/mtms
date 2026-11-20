package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OpeningInventoryRecordDto(
        UUID id,
        UUID branchId,
        LocalDate openingDate,
        int totalItems,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalValue,
        String status,
        UUID approvedBy,
        String approvedByName,
        Instant approvedAt,
        String note
) {
}
