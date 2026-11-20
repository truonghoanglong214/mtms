package com.mtms.inventory.application.dto;

import java.util.List;

public record StockMovementPageResponse(
        List<StockMovementLedgerDto> items,
        long totalItems
) {}
