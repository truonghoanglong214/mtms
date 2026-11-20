package com.mtms.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseCommand(
        @NotNull(message = "Ngày chi không được để trống")
        LocalDate expenseDate,

        @NotNull(message = "Danh mục chi phí không được để trống")
        UUID categoryId,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        BigDecimal amount,

        @NotNull(message = "Hình thức thanh toán không được để trống")
        Boolean isPaid,

        UUID paymentAccountId,
        UUID evidenceFileId,

        @NotBlank(message = "Lý do chi phí không được để trống")
        String reason,

        boolean confirmImmediately
) {
}
