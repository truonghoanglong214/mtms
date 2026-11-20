package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OpeningPreviewResponse(
        UUID branchId,
        LocalDate openingDate,
        int totalItems,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalValuation,
        boolean hasErrors,
        List<OpeningPreviewRowDto> previewRows
) {
}
