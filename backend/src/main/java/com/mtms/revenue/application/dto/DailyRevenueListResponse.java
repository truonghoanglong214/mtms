package com.mtms.revenue.application.dto;

import java.util.List;

public record DailyRevenueListResponse(
        List<DailyRevenueReportDto> items,
        long totalItems
) {
}
