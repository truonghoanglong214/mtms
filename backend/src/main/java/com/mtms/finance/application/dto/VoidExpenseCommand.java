package com.mtms.finance.application.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidExpenseCommand(
        @NotBlank(message = "Lý do hủy không được để trống")
        String reason
) {
}
