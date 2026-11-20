package com.mtms.finance.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.finance.application.OpeningBalanceService;
import com.mtms.finance.application.dto.CreateOpeningBatchCommand;
import com.mtms.finance.application.dto.OpeningBalanceBatchDto;
import com.mtms.finance.application.dto.OpeningBalanceStatusResponse;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009 / FE-P0-16
 * REST controller for opening balance import, working capital reconciliation, and cutover batch posting.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/finance/opening-balances")
public class OpeningBalanceController {

    private final OpeningBalanceService openingBalanceService;
    private final BranchAuthorizationService branchAuthorizationService;

    public OpeningBalanceController(
            OpeningBalanceService openingBalanceService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.openingBalanceService = openingBalanceService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping("/status")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<OpeningBalanceStatusResponse> getOpeningStatus(@PathVariable UUID branchId) {
        OpeningBalanceStatusResponse response = openingBalanceService.getOpeningStatus(branchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<OpeningBalanceBatchDto> getOpeningBatchDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        OpeningBalanceBatchDto response = openingBalanceService.getOpeningBatchDetail(branchId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER')")
    @Idempotent(required = false)
    public ResponseEntity<OpeningBalanceBatchDto> saveOpeningBatch(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateOpeningBatchCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        OpeningBalanceBatchDto response = openingBalanceService.saveOpeningBatch(branchId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'OWNER')")
    @Idempotent(required = false)
    public ResponseEntity<OpeningBalanceBatchDto> postOpeningBatch(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        OpeningBalanceBatchDto response = openingBalanceService.postOpeningBatch(branchId, id, currentUserId);
        return ResponseEntity.ok(response);
    }
}
