package com.mtms.finance.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.finance.application.PayableSettlementService;
import com.mtms.finance.application.dto.CreateManualPayableCommand;
import com.mtms.finance.application.dto.PayableDetailDto;
import com.mtms.finance.application.dto.PayableDto;
import com.mtms.finance.application.dto.PayablePaymentResponseDto;
import com.mtms.finance.application.dto.SettlePayableCommand;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.inventory.domain.PayableStatus;
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
 * P0-FIN-05 / BR-FIN-02 / TC-009
 * REST controller for branch payables ledger and partial/full debt settlement.
 */
@RestController("financePayableController")
@RequestMapping("/api/v1/branches/{branchId}/payables")
public class PayableController {

    private final PayableSettlementService payableSettlementService;
    private final BranchAuthorizationService branchAuthorizationService;

    public PayableController(
            PayableSettlementService payableSettlementService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.payableSettlementService = payableSettlementService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<Page<PayableDto>> getPayables(
            @PathVariable UUID branchId,
            @RequestParam(required = false) PayableStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<PayableDto> response = payableSettlementService.getPayables(branchId, status, search, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<PayableDto> createManualPayable(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateManualPayableCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        PayableDto response = payableSettlementService.createManualPayable(branchId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<PayableDetailDto> getPayableDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        PayableDetailDto response = payableSettlementService.getPayableDetail(branchId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<PayablePaymentResponseDto> settlePayable(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody SettlePayableCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        PayablePaymentResponseDto response = payableSettlementService.settlePayable(branchId, id, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
