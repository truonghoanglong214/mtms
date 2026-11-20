package com.mtms.finance.application.dto;

import com.mtms.finance.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * Command to record a tax payment installment.
 */
public record PayTaxObligationCommand(
        @NotNull(message = "Ngày nộp thuế không được để trống")
        LocalDate paymentDate,

        @NotNull(message = "Số tiền nộp không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền nộp phải lớn hơn 0")
        BigDecimal amount,

        PaymentMethod paymentMethod,

        UUID financialAccountId,

        @Size(max = 100, message = "Số chứng từ/giấy nộp tiền không quá 100 ký tự")
        String receiptNumber,

        String note,

        UUID evidenceFileId
) {}
