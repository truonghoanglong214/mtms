package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OpeningPreviewRowDto(
        int rowNumber,
        UUID itemId,
        String itemCode,
        String itemName,
        String unitCode,
        String unitName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal quantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal unitCost,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal lineTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal calculatedWac,
        String lotCode,
        LocalDate expiresOn,
        boolean lotTrackingRequired,
        String status,
        List<String> errors
) {
}
