package com.mtms.revenue.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDailyRevenueCommand(
        @NotNull(message = "businessDate must not be null")
        LocalDate businessDate,
        BigDecimal cashAmount,
        BigDecimal bankTransferAmount,
        BigDecimal deliveryAppNetAmount,
        BigDecimal otherAdjustmentAmount,
        BigDecimal totalRevenue,
        UUID evidenceFileId,
        String note,
        Boolean submitImmediately
) {
}
