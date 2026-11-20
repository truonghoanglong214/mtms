package com.mtms.revenue.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfirmRevenueCommand(
        String adjustmentReason,
        BigDecimal cashAmount,
        BigDecimal bankTransferAmount,
        BigDecimal deliveryAppNetAmount,
        BigDecimal otherAdjustmentAmount,
        BigDecimal totalRevenue,
        UUID evidenceFileId,
        String note
) {
}
