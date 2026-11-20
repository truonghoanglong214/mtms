package com.mtms.finance.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * Command to create a new cash/bank reconciliation snapshot.
 */
public record CreateReconciliationCommand(
        @NotNull(message = "Tài khoản đối soát không được để trống")
        UUID accountId,

        @NotNull(message = "Ngày đối soát không được để trống")
        LocalDate reconciliationDate,

        @NotNull(message = "Số dư thực tế kiểm đếm không được để trống")
        BigDecimal observedBalance,

        String note,

        UUID evidenceFileId
) {}
