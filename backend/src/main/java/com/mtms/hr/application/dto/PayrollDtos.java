package com.mtms.hr.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mtms.hr.domain.AdjustmentCode;
import com.mtms.hr.domain.PayrollPeriodStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-HR-05 & P0-HR-06 DTO and Command records.
 */
public final class PayrollDtos {

    private PayrollDtos() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RateSliceDto(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal rate,
            Double hours,
            BigDecimal amount
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CalculationSnapshotDto(
            List<RateSliceDto> slices,
            BigDecimal standardWorkdays,
            BigDecimal payableWorkdays,
            BigDecimal rate,
            BigDecimal amount
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PolicySnapshotDto(
            String rateType,
            BigDecimal currentRate,
            BigDecimal standardWorkdays
    ) {}

    public record PayrollAdjustmentDto(
            UUID id,
            UUID payrollLineId,
            String adjustmentCode,
            BigDecimal signedAmount,
            String explanation,
            UUID approvedBy,
            Instant createdAt
    ) {}

    public record PayrollLineDto(
            UUID id,
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
            Object policySnapshot,
            Object calculationSnapshot,
            List<PayrollAdjustmentDto> adjustments
    ) {}

    public record PayrollPeriodDto(
            UUID id,
            UUID branchId,
            LocalDate monthStart,
            Integer revision,
            Boolean isCurrent,
            PayrollPeriodStatus status,
            BigDecimal totalGrossSalary,
            BigDecimal totalBaseSalary,
            BigDecimal totalAdjustments,
            Integer employeeCount,
            String note,
            UUID approvedBy,
            Instant approvedAt,
            List<PayrollLineDto> lines
    ) {}

    public record GeneratePayrollCommand(
            @NotNull LocalDate monthStart
    ) {}

    public record CreateAdjustmentCommand(
            @NotNull AdjustmentCode adjustmentCode,
            @NotNull BigDecimal signedAmount,
            @NotBlank String explanation
    ) {}
}
