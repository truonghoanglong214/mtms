package com.mtms.inventory.application.dto;

import java.util.List;

public record PurchaseReceiptListResponse(
        List<PurchaseReceiptDto> items,
        long totalItems
) {
}
