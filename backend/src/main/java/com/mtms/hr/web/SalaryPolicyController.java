package com.mtms.hr.web;

import com.mtms.hr.application.SalaryPolicyService;
import com.mtms.hr.application.dto.HrDtos.CreateSalaryPolicyCommand;
import com.mtms.hr.application.dto.HrDtos.SalaryPolicyDto;
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
import java.util.UUID;

/**
 * P0-HR-02 / BR-HR-01 / TC-013
 * REST controller for effective salary and wage policy management.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/hr/employees/{id}")
public class SalaryPolicyController {

    private final SalaryPolicyService salaryPolicyService;

    public SalaryPolicyController(SalaryPolicyService salaryPolicyService) {
        this.salaryPolicyService = salaryPolicyService;
    }

    @PostMapping("/salary-policies")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<SalaryPolicyDto> createSalaryPolicy(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateSalaryPolicyCommand command
    ) {
        SalaryPolicyDto response = salaryPolicyService.createSalaryPolicy(branchId, id, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/effective-salary-policy")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<SalaryPolicyDto> getEffectiveSalaryPolicy(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        SalaryPolicyDto response = salaryPolicyService.getEffectiveSalaryPolicy(branchId, id, date);
        return ResponseEntity.ok(response);
    }
}
