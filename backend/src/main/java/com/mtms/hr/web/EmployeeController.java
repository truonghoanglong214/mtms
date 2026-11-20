package com.mtms.hr.web;

import com.mtms.hr.application.EmployeeService;
import com.mtms.hr.application.dto.HrDtos.CreateAssignmentCommand;
import com.mtms.hr.application.dto.HrDtos.CreateEmployeeCommand;
import com.mtms.hr.application.dto.HrDtos.CreatedEmployeeResponse;
import com.mtms.hr.application.dto.HrDtos.EmployeeAssignmentDto;
import com.mtms.hr.application.dto.HrDtos.EmployeeDetailDto;
import com.mtms.hr.application.dto.HrDtos.EmployeeSummaryDto;
import com.mtms.hr.application.dto.HrDtos.PagedResponse;
import com.mtms.hr.application.dto.HrDtos.UpdateEmployeeCommand;
import com.mtms.hr.domain.EmployeeStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * P0-HR-01 / BR-HR-03 / TC-015
 * REST controller for workforce employee directory, profiles, and branch allocations.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/hr/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<PagedResponse<EmployeeSummaryDto>> getEmployees(
            @PathVariable UUID branchId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PagedResponse<EmployeeSummaryDto> response =
                employeeService.getEmployees(branchId, search, status, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<CreatedEmployeeResponse> createEmployee(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateEmployeeCommand command
    ) {
        CreatedEmployeeResponse response = employeeService.createEmployee(branchId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<EmployeeDetailDto> getEmployee(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        EmployeeDetailDto detail = employeeService.getEmployeeDetail(branchId, id);
        return ResponseEntity.ok(detail);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<EmployeeDetailDto> updateEmployee(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeCommand command
    ) {
        EmployeeDetailDto detail = employeeService.updateEmployee(branchId, id, command);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<EmployeeAssignmentDto> assignBranch(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateAssignmentCommand command
    ) {
        EmployeeAssignmentDto assignment = employeeService.assignBranch(branchId, id, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }
}
