package com.mtms.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateExpenseConfigCommand(
        @NotNull(message = "Hạn mức cảnh báo không được để trống")
        @DecimalMin(value = "0.01", message = "Hạn mức cảnh báo phải lớn hơn 0")
        BigDecimal alertThresholdAmount,

        @NotNull(message = "Hạn mức bắt buộc chứng từ không được để trống")
        @DecimalMin(value = "0.01", message = "Hạn mức bắt buộc chứng từ phải lớn hơn 0")
        BigDecimal evidenceRequiredThreshold
) {
}
