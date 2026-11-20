package com.mtms.inventory.application.dto;

import java.util.List;

public record StockDocumentListResponse(
        List<StockDocumentDto> items,
        long totalItems
) {}
