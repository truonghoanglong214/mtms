package com.mtms.hr.web;

import com.mtms.common.error.AccessForbiddenException;
import com.mtms.hr.application.PayrollPaymentService;
import com.mtms.hr.application.dto.PayrollPaymentDtos.BulkPaySalaryCommand;
import com.mtms.hr.application.dto.PayrollPaymentDtos.BulkPaySalaryResponse;
import com.mtms.hr.application.dto.PayrollPaymentDtos.PaySalaryCommand;
import com.mtms.hr.application.dto.PayrollPaymentDtos.PayrollPaymentDto;
import com.mtms.hr.application.dto.PayrollPaymentDtos.PayslipDto;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
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
 * P0-HR-07, P0-HR-08 & P0-HR-09 REST Controller.
 * Authoritative: docs/api/contracts/P0-HR-07-09.md
 * Handles employee payroll settlements, cash ledger integration, and privacy-guarded payslip access.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/hr")
@PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
public class PayrollPaymentController {

    private final PayrollPaymentService payrollPaymentService;
    private final BranchAuthorizationService branchAuth;
    private final EmployeeJpaRepository employeeRepository;

    public PayrollPaymentController(
            PayrollPaymentService payrollPaymentService,
            BranchAuthorizationService branchAuth,
            EmployeeJpaRepository employeeRepository
    ) {
        this.payrollPaymentService = payrollPaymentService;
        this.branchAuth = branchAuth;
        this.employeeRepository = employeeRepository;
    }

    /**
     * 3.1 POST /api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/pay
     * Records a partial or full salary payment for a specific payroll line.
     */
    @PostMapping("/payroll-lines/{lineId}/pay")
    public ResponseEntity<PayrollPaymentDto> paySalary(
            @PathVariable UUID branchId,
            @PathVariable UUID lineId,
            @Valid @RequestBody PaySalaryCommand command
    ) {
        UUID currentUserId = branchAuth.getCurrentUserId();
        PayrollPaymentDto response = payrollPaymentService.paySalary(branchId, lineId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 3.2 POST /api/v1/branches/{branchId}/hr/payroll-periods/{periodId}/bulk-pay
     * Settles multiple payroll lines in bulk for an approved payroll period.
     */
    @PostMapping("/payroll-periods/{periodId}/bulk-pay")
    public ResponseEntity<BulkPaySalaryResponse> bulkPaySalary(
            @PathVariable UUID branchId,
            @PathVariable UUID periodId,
            @Valid @RequestBody BulkPaySalaryCommand command
    ) {
        UUID currentUserId = branchAuth.getCurrentUserId();
        BulkPaySalaryResponse response = payrollPaymentService.bulkPaySalary(branchId, periodId, command, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3.3 GET /api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/payments
     * Retrieves payment settlement history for a specific payroll line.
     */
    @GetMapping("/payroll-lines/{lineId}/payments")
    public ResponseEntity<List<PayrollPaymentDto>> getPaymentsForLine(
            @PathVariable UUID branchId,
            @PathVariable UUID lineId
    ) {
        List<PayrollPaymentDto> response = payrollPaymentService.getPaymentsForLine(branchId, lineId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3.4 GET /api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/payslip
     * Retrieves detailed payslip statement for a specific payroll line.
     * Enforces TC-017 privacy guard: non-management users can only view their own statement.
     */
    @GetMapping("/payroll-lines/{lineId}/payslip")
    public ResponseEntity<PayslipDto> getPayslip(
            @PathVariable UUID branchId,
            @PathVariable UUID lineId
    ) {
        UUID currentUserId = branchAuth.getCurrentUserId();
        boolean isManagerOrAbove = branchAuth.hasBranchAccess(branchId, "OWNER", "BRANCH_OWNER", "MANAGER", "ADMIN", "FINANCE");

        PayslipDto payslip = payrollPaymentService.getPayslip(branchId, lineId);
        if (!isManagerOrAbove) {
            employeeRepository.findByUserId(currentUserId).ifPresentOrElse(emp -> {
                if (!emp.getId().equals(payslip.employeeId())) {
                    throw new AccessForbiddenException("Nhân viên chỉ được xem phiếu lương của chính mình (TC-017)");
                }
            }, () -> {
                throw new AccessForbiddenException("Tài khoản chưa được liên kết với hồ sơ nhân viên");
            });
        }

        return ResponseEntity.ok(payslip);
    }

    /**
     * 3.5 GET /api/v1/branches/{branchId}/hr/my-payslips?month=YYYY-MM-01
     * TC-017 Privacy Guard: Allows authenticated employees to retrieve only their own approved payslips.
     */
    @GetMapping("/my-payslips")
    public ResponseEntity<List<PayslipDto>> getMyPayslips(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        UUID currentUserId = branchAuth.getCurrentUserId();
        List<PayslipDto> response = payrollPaymentService.getMyPayslips(branchId, currentUserId, month);
        return ResponseEntity.ok(response);
    }
}
