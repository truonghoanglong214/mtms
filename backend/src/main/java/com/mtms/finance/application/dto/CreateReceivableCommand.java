package com.mtms.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-05
 * Command to create a receivable record.
 */
public record CreateReceivableCommand(
        @NotBlank(message = "Tên đối tượng phải thu không được để trống")
        String counterpartyLabel,

        @NotNull(message = "Ngày phát sinh không được để trống")
        LocalDate incurredDate,

        LocalDate dueDate,

        @NotNull(message = "Số tiền phải thu không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền phải thu phải lớn hơn 0")
        BigDecimal originalAmount,

        String notes,

        String sourceType,

        UUID sourceId
) {
}
