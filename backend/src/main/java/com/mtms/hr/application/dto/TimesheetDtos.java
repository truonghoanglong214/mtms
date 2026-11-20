package com.mtms.hr.application.dto;

import com.mtms.hr.domain.TimesheetStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-HR-03 & P0-HR-04 / BR-HR-02 / TC-017
 * DTOs and command definitions for Timesheets and Approvals.
 */
public final class TimesheetDtos {

    private TimesheetDtos() {}

    public record TimesheetEntryDto(
            UUID id,
            UUID employeeId,
            String staffCode,
            String employeeName,
            String positionName,
            LocalDate workDate,
            Integer rawMinutes,
            Integer approvedMinutes,
            BigDecimal payableWorkdays,
            String sourceType,
            TimesheetStatus status,
            String note,
            UUID approvedBy,
            String approverName,
            Instant approvedAt,
            String approvalNote
    ) {}

    public record MonthlyTimesheetSummaryDto(
            UUID employeeId,
            String staffCode,
            String employeeName,
            String positionName,
            Integer totalApprovedMinutes,
            BigDecimal totalApprovedHours,
            BigDecimal totalPayableWorkdays,
            int approvedDaysCount,
            int pendingDaysCount,
            int rejectedDaysCount
    ) {}

    public record CreateTimesheetCommand(
            @NotNull(message = "employeeId is required") UUID employeeId,
            @NotNull(message = "workDate is required") LocalDate workDate,
            Integer rawMinutes,
            @NotNull(message = "approvedMinutes is required") Integer approvedMinutes,
            @NotNull(message = "payableWorkdays is required") BigDecimal payableWorkdays,
            TimesheetStatus status,
            String note
    ) {}

    public record BulkTimesheetItemCommand(
            @NotNull(message = "employeeId is required") UUID employeeId,
            Integer rawMinutes,
            @NotNull(message = "approvedMinutes is required") Integer approvedMinutes,
            @NotNull(message = "payableWorkdays is required") BigDecimal payableWorkdays,
            TimesheetStatus status,
            String note
    ) {}

    public record BulkTimesheetCommand(
            @NotNull(message = "workDate is required") LocalDate workDate,
            @NotNull(message = "entries is required") List<@Valid BulkTimesheetItemCommand> entries
    ) {}

    public record ApproveTimesheetCommand(
            String approvalNote
    ) {}

    public record BulkApproveCommand(
            @NotNull(message = "entryIds is required") List<UUID> entryIds,
            String approvalNote
    ) {}

    public record BulkApproveResponse(
            int approvedCount,
            int failedCount
    ) {}

    public record RejectTimesheetCommand(
            String rejectionReason
    ) {}
}
