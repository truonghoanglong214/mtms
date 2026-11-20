package com.mtms.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InternalTransferCommand(
        @NotNull(message = "Tài khoản nguồn không được để trống")
        UUID fromAccountId,

        @NotNull(message = "Tài khoản đích không được để trống")
        UUID toAccountId,

        @NotNull(message = "Số tiền chuyển không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền chuyển phải lớn hơn 0")
        BigDecimal amount,

        @NotNull(message = "Ngày kinh doanh không được để trống")
        LocalDate businessDate,

        String note,

        UUID evidenceFileId
) {}
