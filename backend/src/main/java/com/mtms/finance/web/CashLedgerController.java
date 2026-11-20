package com.mtms.finance.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.finance.application.CashLedgerService;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.CashLedgerPageResponse;
import com.mtms.finance.application.dto.InternalTransferCommand;
import com.mtms.finance.application.dto.InternalTransferResponseDto;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.domain.SourceType;
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

@RestController
@RequestMapping("/api/v1/branches/{branchId}/finance")
public class CashLedgerController {

    private final CashLedgerService cashLedgerService;
    private final BranchAuthorizationService branchAuthorizationService;

    public CashLedgerController(
            CashLedgerService cashLedgerService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.cashLedgerService = cashLedgerService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping("/ledger")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<CashLedgerPageResponse> getLedger(
            @PathVariable UUID branchId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        CashLedgerPageResponse response = cashLedgerService.getLedgerEntries(
                branchId,
                accountId,
                categoryId,
                sourceType,
                startDate,
                endDate,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfers")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<InternalTransferResponseDto> transferFunds(
            @PathVariable UUID branchId,
            @Valid @RequestBody InternalTransferCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        InternalTransferResponseDto response = cashLedgerService.recordInternalTransfer(branchId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/entries")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<CashLedgerEntryDto> postEntry(
            @PathVariable UUID branchId,
            @Valid @RequestBody PostLedgerEntryCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        CashLedgerEntryDto response = cashLedgerService.postLedgerEntry(branchId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
