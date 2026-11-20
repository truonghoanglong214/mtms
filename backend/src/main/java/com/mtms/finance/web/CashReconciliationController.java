package com.mtms.finance.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.finance.application.CashReconciliationService;
import com.mtms.finance.application.dto.AdjustReconciliationCommand;
import com.mtms.finance.application.dto.CashReconciliationDto;
import com.mtms.finance.application.dto.CashReconciliationPageResponse;
import com.mtms.finance.application.dto.CreateReconciliationCommand;
import com.mtms.finance.application.dto.ExplainDiscrepancyCommand;
import com.mtms.finance.domain.ReconciliationDisposition;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
 * P0-FIN-06 / BR-FIN-06 / TC-012 / FE-P0-14
 * REST controller for cash and bank account reconciliations.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/finance/reconciliations")
public class CashReconciliationController {

    private final CashReconciliationService reconciliationService;
    private final BranchAuthorizationService branchAuthorizationService;

    public CashReconciliationController(
            CashReconciliationService reconciliationService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.reconciliationService = reconciliationService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<CashReconciliationPageResponse> listReconciliations(
            @PathVariable UUID branchId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ReconciliationDisposition disposition,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        CashReconciliationPageResponse response = reconciliationService.listReconciliations(
                branchId, accountId, startDate, endDate, disposition, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<CashReconciliationDto> getReconciliationDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        CashReconciliationDto response = reconciliationService.getReconciliationDetail(branchId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<CashReconciliationDto> createSnapshot(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateReconciliationCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }
        CashReconciliationDto response = reconciliationService.createSnapshot(branchId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/explain")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<CashReconciliationDto> explainDiscrepancy(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody ExplainDiscrepancyCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }
        CashReconciliationDto response = reconciliationService.explainDiscrepancy(branchId, id, command, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'OWNER')")
    @Idempotent(required = false)
    public ResponseEntity<CashReconciliationDto> adjustVariance(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @RequestBody(required = false) AdjustReconciliationCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }
        AdjustReconciliationCommand effectiveCommand = command != null ? command : new AdjustReconciliationCommand(null);
        CashReconciliationDto response = reconciliationService.adjustVariance(branchId, id, effectiveCommand, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'OWNER')")
    @Idempotent(required = false)
    public ResponseEntity<CashReconciliationDto> closeReconciliation(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }
        CashReconciliationDto response = reconciliationService.closeReconciliation(branchId, id, currentUserId);
        return ResponseEntity.ok(response);
    }
}
