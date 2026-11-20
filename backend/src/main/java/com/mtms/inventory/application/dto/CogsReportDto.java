package com.mtms.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CogsReportDto(
        UUID branchId,
        String branchName,
        String period,
        LocalDate startDate,
        LocalDate endDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal beginningStockValue,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal periodPurchasesTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal periodCogsTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal periodAdjustmentsTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal calculatedEndingStockValue,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal actualEndingStockValue,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal reconciliationDiscrepancy,
        boolean reconciled,
        boolean noDoubleCostVerified,
        String accountingExplanation,
        List<CogsItemDetailDto> items
) {}
