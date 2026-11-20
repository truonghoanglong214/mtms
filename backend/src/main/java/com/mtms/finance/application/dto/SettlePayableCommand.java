package com.mtms.finance.application.dto;

import com.mtms.finance.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-05 / BR-FIN-02 / TC-009
 * Command to execute partial or full debt settlement on an outstanding payable.
 */
public record SettlePayableCommand(
        @NotNull(message = "Tài khoản trích tiền không được để trống")
        UUID accountId,

        @NotNull(message = "Số tiền thanh toán không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền thanh toán phải lớn hơn 0")
        BigDecimal amount,

        @NotNull(message = "Ngày thanh toán không được để trống")
        LocalDate paidOn,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod paymentMethod,

        String bankReference,

        UUID evidenceFileId
) {
}
