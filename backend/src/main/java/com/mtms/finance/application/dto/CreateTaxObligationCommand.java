package com.mtms.finance.application.dto;

import com.mtms.finance.domain.TaxObligationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * Command to create a manual tax obligation.
 */
public record CreateTaxObligationCommand(
        @NotBlank(message = "Mã nghĩa vụ thuế không được để trống")
        @Size(max = 60, message = "Mã nghĩa vụ thuế không quá 60 ký tự")
        String obligationCode,

        @NotNull(message = "Loại thuế không được để trống")
        TaxObligationType obligationType,

        @Size(max = 200, message = "Tên cơ quan thuế không quá 200 ký tự")
        String taxAuthorityName,

        @NotNull(message = "Ngày bắt đầu kỳ tính thuế không được để trống")
        LocalDate taxPeriodStart,

        @NotNull(message = "Ngày kết thúc kỳ tính thuế không được để trống")
        LocalDate taxPeriodEnd,

        LocalDate dueDate,

        @NotNull(message = "Số tiền phải nộp không được để trống")
        @DecimalMin(value = "0.00", message = "Số tiền phải nộp không được âm")
        BigDecimal amountDue,

        Boolean isBranchSpecific,

        String note,

        UUID evidenceFileId
) {}
