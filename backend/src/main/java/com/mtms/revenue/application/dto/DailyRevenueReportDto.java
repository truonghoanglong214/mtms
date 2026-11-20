package com.mtms.revenue.application.dto;

import com.mtms.revenue.domain.RevenueReportStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailyRevenueReportDto(
        UUID id,
        UUID branchId,
        LocalDate businessDate,
        int revision,
        boolean isCurrent,
        String cashAmount,
        String bankTransferAmount,
        String deliveryAppNetAmount,
        String otherAdjustmentAmount,
        String totalRevenue,
        RevenueReportStatus status,
        String submittedByName,
        Instant submittedAt,
        String confirmedByName,
        Instant confirmedAt,
        String adjustmentReason,
        UUID evidenceFileId,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
