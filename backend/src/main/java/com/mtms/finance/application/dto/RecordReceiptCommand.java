package com.mtms.finance.application.dto;

import com.mtms.finance.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-05
 * Command to record a receipt of money against a receivable.
 */
public record RecordReceiptCommand(
        @NotNull(message = "Tài khoản nhận tiền không được để trống")
        UUID accountId,

        @NotNull(message = "Số tiền thu không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền thu phải lớn hơn 0")
        BigDecimal amount,

        @NotNull(message = "Ngày thu tiền không được để trống")
        LocalDate receivedOn,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod paymentMethod,

        String bankReference,

        UUID evidenceFileId
) {
}
