package com.mtms.organization.web;

import com.mtms.common.api.ApiResponse;
import com.mtms.organization.application.BranchBankService;
import com.mtms.organization.application.dto.BranchBankAccountDto;
import com.mtms.organization.application.dto.BranchDto;
import com.mtms.organization.application.dto.BranchReadinessDto;
import com.mtms.organization.application.dto.CreateBankAccountCommand;
import com.mtms.organization.application.exception.BranchNotReadyException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * P0-ORG-07 / BR-ORG-03 / TC-003
 * REST endpoints for branch dedicated bank account onboarding and go-live readiness gate.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}")
public class BranchBankController {

    private final BranchBankService branchBankService;

    public BranchBankController(BranchBankService branchBankService) {
        this.branchBankService = branchBankService;
    }

    @GetMapping("/bank-accounts")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<BranchBankAccountDto>>> listBankAccounts(
            @PathVariable UUID branchId) {
        List<BranchBankAccountDto> accounts = branchBankService.listBankAccounts(branchId);
        return ResponseEntity.ok(ApiResponse.ok(accounts));
    }

    @PostMapping("/bank-accounts")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<BranchBankAccountDto>> addBankAccount(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateBankAccountCommand command) {
        BranchBankAccountDto created = branchBankService.addBankAccount(branchId, command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Bank account added successfully"));
    }

    @PatchMapping("/bank-accounts/{accountId}/primary")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<BranchBankAccountDto>> setPrimaryAccount(
            @PathVariable UUID branchId,
            @PathVariable UUID accountId) {
        BranchBankAccountDto updated = branchBankService.setPrimaryAccount(branchId, accountId);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Primary bank account updated successfully"));
    }

    @GetMapping("/readiness")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<BranchReadinessDto>> checkReadiness(
            @PathVariable UUID branchId) {
        BranchReadinessDto readiness = branchBankService.checkBranchReadiness(branchId);
        return ResponseEntity.ok(ApiResponse.ok(readiness));
    }

    @PatchMapping("/activate")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<BranchDto>> activateBranch(
            @PathVariable UUID branchId) {
        BranchDto activated = branchBankService.activateBranch(branchId);
        return ResponseEntity.ok(ApiResponse.ok(activated, "Branch activated successfully"));
    }

    @ExceptionHandler(BranchNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleBranchNotReadyException(BranchNotReadyException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("urn:mtms:error:branch-not-ready"));
        problemDetail.setTitle("Branch Not Ready For Activation");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("branchId", ex.getBranchId());
        problemDetail.setProperty("missingRequirements", ex.getMissingRequirements());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }
}
