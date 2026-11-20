package com.mtms.finance.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-FIN-08 / BR-FIN-08 / ADR-009
 * Command to create or save a draft opening balance batch.
 */
public record CreateOpeningBatchCommand(
        @NotNull(message = "Ngày chốt số dư không được để trống")
        LocalDate cutoverDate,

        String note,

        UUID evidenceFileId,

        @Valid
        List<AccountBalanceItem> accountBalances
) {
    public record AccountBalanceItem(
            @NotNull(message = "Mã tài khoản không được để trống")
            UUID accountId,

            @NotNull(message = "Số dư đầu kỳ không được để trống")
            @DecimalMin(value = "0.00", message = "Số dư đầu kỳ không được âm")
            BigDecimal openingBalance
    ) {}
}
