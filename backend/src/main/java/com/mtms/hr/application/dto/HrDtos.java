package com.mtms.hr.application.dto;

import com.mtms.hr.domain.EmployeeStatus;
import com.mtms.hr.domain.RateType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-HR-01 & P0-HR-02 DTOs and command definitions.
 */
public final class HrDtos {

    private HrDtos() {}

    public record EmployeeSummaryDto(
            UUID id,
            String staffCode,
            String fullName,
            String positionName,
            String phoneNumber,
            String email,
            LocalDate hiredOn,
            LocalDate leftOn,
            EmployeeStatus status,
            boolean isPrimaryBranch,
            RateType activeRateType,
            BigDecimal activeRateAmount,
            Instant createdAt
    ) {}

    public record EmployeeDetailDto(
            UUID id,
            String staffCode,
            String fullName,
            String positionName,
            String phoneNumber,
            String email,
            LocalDate hiredOn,
            LocalDate leftOn,
            EmployeeStatus status,
            String notes,
            List<EmployeeAssignmentDto> assignments,
            List<SalaryPolicyDto> salaryPolicies
    ) {}

    public record EmployeeAssignmentDto(
            UUID id,
            UUID branchId,
            String branchName,
            boolean isPrimary,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes,
            Instant createdAt
    ) {}

    public record SalaryPolicyDto(
            UUID id,
            UUID employeeId,
            UUID branchId,
            RateType rateType,
            BigDecimal rateAmount,
            BigDecimal standardWorkdays,
            Integer paidMinutesRoundingBlock,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes,
            UUID approvedBy,
            Instant createdAt
    ) {}

    public record CreatedEmployeeResponse(
            UUID id,
            String staffCode,
            String fullName,
            String positionName,
            String phoneNumber,
            String email,
            LocalDate hiredOn,
            LocalDate leftOn,
            EmployeeStatus status,
            String notes,
            Instant createdAt
    ) {}

    public record CreateEmployeeCommand(
            @NotBlank(message = "staffCode is required") String staffCode,
            @NotBlank(message = "fullName is required") String fullName,
            @NotBlank(message = "positionName is required") String positionName,
            String phoneNumber,
            String email,
            @NotNull(message = "hiredOn is required") LocalDate hiredOn,
            LocalDate leftOn,
            String notes,
            @Valid InitialSalaryPolicyCommand initialSalaryPolicy
    ) {}

    public record InitialSalaryPolicyCommand(
            @NotNull(message = "rateType is required") RateType rateType,
            @NotNull(message = "rateAmount is required") BigDecimal rateAmount,
            BigDecimal standardWorkdays,
            Integer paidMinutesRoundingBlock,
            LocalDate effectiveFrom,
            String notes
    ) {}

    public record UpdateEmployeeCommand(
            @NotBlank(message = "fullName is required") String fullName,
            @NotBlank(message = "positionName is required") String positionName,
            String phoneNumber,
            String email,
            EmployeeStatus status,
            LocalDate leftOn,
            String notes
    ) {}

    public record CreateAssignmentCommand(
            @NotNull(message = "targetBranchId is required") UUID targetBranchId,
            Boolean isPrimary,
            @NotNull(message = "effectiveFrom is required") LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes
    ) {}

    public record CreateSalaryPolicyCommand(
            @NotNull(message = "rateType is required") RateType rateType,
            @NotNull(message = "rateAmount is required") BigDecimal rateAmount,
            BigDecimal standardWorkdays,
            Integer paidMinutesRoundingBlock,
            @NotNull(message = "effectiveFrom is required") LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes
    ) {}

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static <T> PagedResponse<T> from(Page<T> page) {
            return new PagedResponse<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }
    }
}
