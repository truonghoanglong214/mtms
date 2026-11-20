package com.mtms.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * P0-FIN-05
 * Command to create a manual payable debt record.
 */
public record CreateManualPayableCommand(
        @NotBlank(message = "Tên đơn vị đối tác không được để trống")
        String counterpartyLabel,

        @NotNull(message = "Ngày phát sinh nợ không được để trống")
        LocalDate incurredDate,

        LocalDate dueDate,

        @NotNull(message = "Số tiền nợ không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền nợ phải lớn hơn 0")
        BigDecimal originalAmount,

        String notes
) {
}
