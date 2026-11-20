package com.mtms.inventory.application.dto;

import java.util.UUID;

public record OpeningInventoryStatusResponse(
        UUID branchId,
        String branchName,
        boolean isOpeningCommitted,
        OpeningInventoryRecordDto openingRecord
) {
}
