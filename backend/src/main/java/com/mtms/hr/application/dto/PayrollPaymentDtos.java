package com.mtms.hr.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mtms.hr.domain.PaymentMethod;
import com.mtms.hr.domain.PayrollPaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-HR-07 & P0-HR-08 DTO and Command records.
 * Authoritative: docs/api/contracts/P0-HR-07-09.md
 */
public final class PayrollPaymentDtos {

    private PayrollPaymentDtos() {}

    public record PayrollPaymentDto(
            UUID id,
            UUID payrollLineId,
            UUID accountId,
            String accountName,
            BigDecimal amount,
            LocalDate paidOn,
            PaymentMethod paymentMethod,
            PayrollPaymentStatus status,
            String externalReference,
            UUID ledgerEntryId,
            String note,
            BigDecimal paidAmountAfter,
            BigDecimal remainingSalary,
            Instant createdAt
    ) {}

    public record PaySalaryCommand(
            @NotNull(message = "Tài khoản không được để trống")
            UUID accountId,

            @NotNull(message = "Số tiền không được để trống")
            @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
            BigDecimal amount,

            @NotNull(message = "Ngày chi trả không được để trống")
            LocalDate paidOn,

            PaymentMethod paymentMethod,

            String externalReference,

            UUID evidenceFileId,

            String note
    ) {}

    public record BulkPaymentItemCommand(
            @NotNull(message = "Dòng lương không được để trống")
            UUID payrollLineId,

            @NotNull(message = "Số tiền không được để trống")
            @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
            BigDecimal amount,

            String externalReference
    ) {}

    public record BulkPaySalaryCommand(
            @NotNull(message = "Tài khoản không được để trống")
            UUID accountId,

            @NotNull(message = "Ngày chi trả không được để trống")
            LocalDate paidOn,

            PaymentMethod paymentMethod,

            @NotEmpty(message = "Danh sách chi trả không được để trống")
            List<@Valid BulkPaymentItemCommand> items,

            String note
    ) {}

    public record BulkPaySalaryResponse(
            BigDecimal totalPaidAmount,
            int paidCount,
            List<UUID> paymentIds
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PayslipDto(
            UUID payrollPeriodId,
            UUID branchId,
            LocalDate monthStart,
            String periodStatus,
            UUID lineId,
            UUID employeeId,
            String staffCode,
            String employeeName,
            String positionName,
            String rateType,
            Integer approvedMinutes,
            BigDecimal payableWorkdays,
            BigDecimal baseSalary,
            BigDecimal adjustmentsTotal,
            BigDecimal grossSalary,
            BigDecimal paidAmount,
            BigDecimal remainingSalary,
            Object policySnapshot,
            Object calculationSnapshot,
            List<PayrollDtos.PayrollAdjustmentDto> adjustments,
            List<PayrollPaymentDto> payments
    ) {}
}
