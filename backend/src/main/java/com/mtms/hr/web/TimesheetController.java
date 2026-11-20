package com.mtms.hr.web;

import com.mtms.hr.application.TimesheetService;
import com.mtms.hr.application.dto.HrDtos.PagedResponse;
import com.mtms.hr.application.dto.TimesheetDtos.ApproveTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.BulkApproveCommand;
import com.mtms.hr.application.dto.TimesheetDtos.BulkApproveResponse;
import com.mtms.hr.application.dto.TimesheetDtos.BulkTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.CreateTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.MonthlyTimesheetSummaryDto;
import com.mtms.hr.application.dto.TimesheetDtos.RejectTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.TimesheetEntryDto;
import com.mtms.hr.domain.TimesheetStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-HR-03 & P0-HR-04 / BR-HR-02 / TC-013 / TC-017
 * REST controller for timesheet recording, bulk shifts, monthly summary, and invariant approval workflow.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/hr/timesheets")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<PagedResponse<TimesheetEntryDto>> getTimesheets(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) TimesheetStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        PagedResponse<TimesheetEntryDto> response = timesheetService.getTimesheets(
                branchId, startDate, endDate, employeeId, status, PageRequest.of(page, size)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly-summary")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<MonthlyTimesheetSummaryDto>> getMonthlySummary(
            @PathVariable UUID branchId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<MonthlyTimesheetSummaryDto> summary = timesheetService.getMonthlySummary(branchId, year, month);
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TimesheetEntryDto> createOrUpdateTimesheet(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateTimesheetCommand command
    ) {
        TimesheetEntryDto response = timesheetService.createOrUpdateTimesheet(branchId, command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<TimesheetEntryDto>> bulkCreateTimesheet(
            @PathVariable UUID branchId,
            @Valid @RequestBody BulkTimesheetCommand command
    ) {
        List<TimesheetEntryDto> response = timesheetService.bulkCreateTimesheet(branchId, command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TimesheetEntryDto> approveTimesheet(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveTimesheetCommand command
    ) {
        TimesheetEntryDto response = timesheetService.approveTimesheet(branchId, id, command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-approve")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<BulkApproveResponse> bulkApprove(
            @PathVariable UUID branchId,
            @Valid @RequestBody BulkApproveCommand command
    ) {
        BulkApproveResponse response = timesheetService.bulkApprove(branchId, command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TimesheetEntryDto> rejectTimesheet(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @RequestBody(required = false) RejectTimesheetCommand command
    ) {
        TimesheetEntryDto response = timesheetService.rejectTimesheet(branchId, id, command);
        return ResponseEntity.ok(response);
    }
}
