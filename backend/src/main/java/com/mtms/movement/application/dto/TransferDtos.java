package com.mtms.movement.application.dto;

import com.mtms.movement.domain.model.TransferStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TransferDtos {

    private TransferDtos() {}

    public record CreateTransferLineRequest(
            @NotNull(message = "Mặt hàng không được để trống")
            UUID itemId,

            UUID lotId,

            @NotNull(message = "Số lượng xuất không được để trống")
            @DecimalMin(value = "0.001", message = "Số lượng xuất phải lớn hơn 0")
            BigDecimal dispatchedBaseQty,

            String notes
    ) {}

    public record CreateTransferRequest(
            @NotNull(message = "Chi nhánh nhận không được để trống")
            UUID toBranchId,

            @NotNull(message = "Ngày yêu cầu không được để trống")
            LocalDate requestedOn,

            Boolean autoDispatch,

            String notes,

            @NotEmpty(message = "Danh sách mặt hàng điều chuyển không được để trống")
            @Valid
            List<CreateTransferLineRequest> lines
    ) {}

    public record ReceiveTransferLineRequest(
            @NotNull(message = "Mã dòng không được để trống")
            UUID lineId,

            @NotNull(message = "Số lượng thực nhận không được để trống")
            @DecimalMin(value = "0.000", message = "Số lượng thực nhận không được âm")
            BigDecimal receivedBaseQty,

            String receiverNote
    ) {}

    public record ReceiveTransferRequest(
            String notes,

            @NotEmpty(message = "Danh sách tiếp nhận không được để trống")
            @Valid
            List<ReceiveTransferLineRequest> lines
    ) {}

    public record TransferLineDto(
            UUID id,
            int lineNumber,
            UUID itemId,
            String itemName,
            String itemCode,
            String baseUnit,
            UUID lotId,
            String lotCode,
            BigDecimal dispatchedBaseQty,
            BigDecimal receivedBaseQty,
            BigDecimal transitVarianceQty,
            BigDecimal senderUnitCost,
            BigDecimal dispatchedLineCost,
            BigDecimal receivedLineCost,
            String receiverNote,
            String notes
    ) {}

    public record TransferDetailDto(
            UUID id,
            String transferNumber,
            UUID businessEntityId,
            UUID fromBranchId,
            String fromBranchName,
            UUID toBranchId,
            String toBranchName,
            LocalDate requestedOn,
            Instant dispatchedAt,
            Instant completedAt,
            TransferStatus status,
            UUID requestedBy,
            String requestedByName,
            UUID dispatchedBy,
            String dispatchedByName,
            UUID receivedBy,
            String receivedByName,
            int totalLines,
            BigDecimal totalDispatchedQuantity,
            BigDecimal totalReceivedQuantity,
            BigDecimal totalTransitVarianceQuantity,
            BigDecimal totalDispatchedCost,
            BigDecimal totalReceivedCost,
            String notes,
            Instant createdAt,
            Instant updatedAt,
            List<TransferLineDto> lines
    ) {}

    public record TransferListResponse(
            List<TransferDetailDto> items,
            long totalElements,
            int totalPages,
            int currentPage
    ) {}

    public record TransferSummaryDto(
            long outboundCount,
            long inboundCount,
            long inTransitCount,
            long discrepancyCount,
            BigDecimal totalDispatchedCost,
            BigDecimal totalReceivedCost
    ) {}
}
