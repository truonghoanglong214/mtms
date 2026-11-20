package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InventoryDashboardDto(
        UUID branchId,
        String branchName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalOnHandQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalInventoryValuation,
        long totalItemsTracked,
        long lowStockItemCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal periodReceiptsTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal periodIssuesTotal,
        List<TopValuedItemDto> topValuedItems
) {}
