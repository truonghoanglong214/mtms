package com.mtms.finance.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.finance.application.ReceivableService;
import com.mtms.finance.application.dto.CreateReceivableCommand;
import com.mtms.finance.application.dto.ReceivableDetailDto;
import com.mtms.finance.application.dto.ReceivableDto;
import com.mtms.finance.application.dto.ReceivableReceiptDto;
import com.mtms.finance.application.dto.RecordReceiptCommand;
import com.mtms.finance.domain.ReceivableStatus;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.UUID;

/**
 * P0-FIN-05
 * REST controller for branch receivables ledger and receipts collection.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/receivables")
public class ReceivableController {

    private final ReceivableService receivableService;
    private final BranchAuthorizationService branchAuthorizationService;

    public ReceivableController(
            ReceivableService receivableService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.receivableService = receivableService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<Page<ReceivableDto>> getReceivables(
            @PathVariable UUID branchId,
            @RequestParam(required = false) ReceivableStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<ReceivableDto> response = receivableService.getReceivables(branchId, status, search, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<ReceivableDto> createReceivable(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateReceivableCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        ReceivableDto response = receivableService.createReceivable(branchId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<ReceivableDetailDto> getReceivableDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        ReceivableDetailDto response = receivableService.getReceivableDetail(branchId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<ReceivableReceiptDto> recordReceipt(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody RecordReceiptCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        ReceivableReceiptDto response = receivableService.recordReceipt(branchId, id, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
