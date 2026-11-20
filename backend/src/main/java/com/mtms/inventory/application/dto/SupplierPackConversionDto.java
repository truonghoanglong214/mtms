package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierPackConversionDto(
        UUID id,
        UUID businessEntityId,
        UUID supplierId,
        String supplierName,
        UUID itemId,
        String itemCode,
        String packName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal baseUnitsPerPack,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant createdAt
) {
}
