package com.mtms.stockcount.application.dto;

import com.mtms.stockcount.domain.model.StockCountStatus;
import com.mtms.stockcount.domain.model.StockCountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class StockCountDtos {

    private StockCountDtos() {}

    public record CreateStockCountRequest(
            @NotNull(message = "Ngày kiểm kê không được để trống")
            LocalDate countedOn,

            @NotNull(message = "Loại kiểm kê không được để trống")
            StockCountType countType,

            String notes,

            List<UUID> itemIds
    ) {}

    public record UpdateCountLineRequest(
            @NotNull(message = "Mã dòng không được để trống")
            UUID lineId,

            @NotNull(message = "Số lượng đếm thực tế không được để trống")
            @DecimalMin(value = "0.000", message = "Số lượng đếm thực tế không được âm")
            BigDecimal countedQty,

            String discrepancyReason,

            String notes
    ) {}

    public record UpdateStockCountRequest(
            String notes,

            @NotEmpty(message = "Danh sách mặt hàng kiểm kê không được để trống")
            @Valid
            List<UpdateCountLineRequest> lines
    ) {}

    public record StockCountLineDto(
            UUID id,
            int lineNumber,
            UUID itemId,
            String itemCode,
            String itemName,
            String baseUnit,
            UUID lotId,
            BigDecimal expectedQty,
            BigDecimal countedQty,
            BigDecimal varianceQty,
            BigDecimal snapshotUnitCost,
            BigDecimal varianceValue,
            String discrepancyReason,
            String notes
    ) {}

    public record StockCountDetailDto(
            UUID id,
            String countNumber,
            UUID businessEntityId,
            UUID branchId,
            String branchName,
            LocalDate countedOn,
            StockCountStatus status,
            StockCountType countType,
            int totalItems,
            BigDecimal totalSurplusQty,
            BigDecimal totalShortageQty,
            BigDecimal totalSurplusValue,
            BigDecimal totalShortageValue,
            BigDecimal netVarianceValue,
            String notes,
            UUID createdBy,
            String createdByName,
            UUID submittedBy,
            String submittedByName,
            Instant submittedAt,
            UUID approvedBy,
            String approvedByName,
            Instant postedAt,
            Instant createdAt,
            Instant updatedAt,
            List<StockCountLineDto> lines
    ) {}

    public record StockCountListResponse(
            List<StockCountDetailDto> items,
            long totalElements,
            int totalPages,
            int currentPage
    ) {}

    public record StockCountSummaryDto(
            long totalCounts,
            long pendingApprovalCount,
            BigDecimal totalSurplusValue,
            BigDecimal totalShortageValue,
            BigDecimal netVarianceValue
    ) {}
}
