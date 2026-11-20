package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.UUID;

public record CogsItemDetailDto(
        UUID itemId,
        String itemCode,
        String itemName,
        String unitCode,
        String unitName,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal beginningQty,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal beginningValue,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal purchasedQty,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal purchasedValue,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal issuedQty,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal cogsValue,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal endingQty,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal endingWac,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal endingValue
) {}
