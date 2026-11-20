package com.mtms.hr.web;

import com.mtms.hr.application.PayrollService;
import com.mtms.hr.application.dto.PayrollDtos.CreateAdjustmentCommand;
import com.mtms.hr.application.dto.PayrollDtos.GeneratePayrollCommand;
import com.mtms.hr.application.dto.PayrollDtos.PayrollAdjustmentDto;
import com.mtms.hr.application.dto.PayrollDtos.PayrollLineDto;
import com.mtms.hr.application.dto.PayrollDtos.PayrollPeriodDto;
import com.mtms.hr.domain.PayrollPeriodStatus;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * P0-HR-05 & P0-HR-06 REST Controller.
 * Authoritative: docs/api/contracts/P0-HR-05-06.md
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/hr")
@PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
public class PayrollController {

    private final PayrollService payrollService;
    private final BranchAuthorizationService branchAuth;

    public PayrollController(PayrollService payrollService, BranchAuthorizationService branchAuth) {
        this.payrollService = payrollService;
        this.branchAuth = branchAuth;
    }

    @GetMapping("/payroll-periods")
    public ResponseEntity<List<PayrollPeriodDto>> getPayrollPeriods(
            @PathVariable UUID branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PayrollPeriodStatus status
    ) {
        List<PayrollPeriodDto> response = payrollService.getPeriods(branchId, year, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payroll-periods/current")
    public ResponseEntity<PayrollPeriodDto> getCurrentPayrollPeriod(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        PayrollPeriodDto response = payrollService.getCurrentPeriod(branchId, month);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payroll-periods/generate")
    public ResponseEntity<PayrollPeriodDto> generateOrRecalculatePayroll(
            @PathVariable UUID branchId,
            @RequestBody(required = false) GeneratePayrollCommand command
    ) {
        LocalDate monthStart = (command != null && command.monthStart() != null)
                ? command.monthStart()
                : LocalDate.now().withDayOfMonth(1);
        PayrollPeriodDto response = payrollService.generateOrRecalculatePayroll(branchId, monthStart);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payroll-periods/{id}")
    public ResponseEntity<PayrollPeriodDto> getPayrollPeriodById(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        PayrollPeriodDto response = payrollService.getPeriodById(branchId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payroll-periods/{id}/lines")
    public ResponseEntity<List<PayrollLineDto>> getPayrollPeriodLines(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        List<PayrollLineDto> response = payrollService.getPeriodLines(branchId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/payroll-lines/{lineId}/adjustments", "/payroll-periods/lines/{lineId}/adjustments"})
    public ResponseEntity<PayrollAdjustmentDto> addAdjustment(
            @PathVariable UUID branchId,
            @PathVariable UUID lineId,
            @Valid @RequestBody CreateAdjustmentCommand command
    ) {
        PayrollAdjustmentDto response = payrollService.addAdjustment(branchId, lineId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping({"/payroll-adjustments/{adjId}", "/payroll-periods/adjustments/{adjId}"})
    public ResponseEntity<Void> deleteAdjustment(
            @PathVariable UUID branchId,
            @PathVariable UUID adjId
    ) {
        payrollService.deleteAdjustment(branchId, adjId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/payroll-periods/{id}/approve")
    public ResponseEntity<PayrollPeriodDto> approvePayrollPeriod(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        UUID currentUserId = branchAuth.getCurrentUserId();
        PayrollPeriodDto response = payrollService.approvePayrollPeriod(branchId, id, currentUserId);
        return ResponseEntity.ok(response);
    }
}
