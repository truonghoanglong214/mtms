package com.mtms.finance.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.finance.application.ExpenseService;
import com.mtms.finance.application.dto.BranchExpenseConfigDto;
import com.mtms.finance.application.dto.CreateExpenseCommand;
import com.mtms.finance.application.dto.ExpenseDetailDto;
import com.mtms.finance.application.dto.ExpenseDto;
import com.mtms.finance.application.dto.UpdateExpenseConfigCommand;
import com.mtms.finance.application.dto.VoidExpenseCommand;
import com.mtms.finance.domain.ExpenseStatus;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-03 / P0-FIN-04 / BR-FIN-07 / TC-011
 * REST controller for operating expense recording, approval, voiding, and configuration.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final BranchAuthorizationService branchAuthorizationService;

    public ExpenseController(
            ExpenseService expenseService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.expenseService = expenseService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<Page<ExpenseDto>> getExpenses(
            @PathVariable UUID branchId,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean reviewFlagOnly,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<ExpenseDto> page = expenseService.getExpenses(
                branchId,
                status,
                categoryId,
                startDate,
                endDate,
                reviewFlagOnly,
                search,
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<ExpenseDto> createExpense(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateExpenseCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        ExpenseDto response = expenseService.createExpense(branchId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<ExpenseDetailDto> getExpenseDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        ExpenseDetailDto response = expenseService.getExpenseDetail(branchId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<ExpenseDto> confirmExpense(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        ExpenseDto response = expenseService.confirmExpense(branchId, id, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<ExpenseDto> voidExpense(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) VoidExpenseCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        ExpenseDto response = expenseService.voidExpense(branchId, id, command, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<BranchExpenseConfigDto> getExpenseConfig(
            @PathVariable UUID branchId
    ) {
        BranchExpenseConfigDto response = expenseService.getExpenseConfig(branchId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/config")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<BranchExpenseConfigDto> updateExpenseConfig(
            @PathVariable UUID branchId,
            @Valid @RequestBody UpdateExpenseConfigCommand command
    ) {
        BranchExpenseConfigDto response = expenseService.updateExpenseConfig(branchId, command);
        return ResponseEntity.ok(response);
    }
}
