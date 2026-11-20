package com.mtms.finance.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.finance.application.TaxObligationService;
import com.mtms.finance.application.dto.CreateTaxObligationCommand;
import com.mtms.finance.application.dto.PayTaxObligationCommand;
import com.mtms.finance.application.dto.TaxObligationDetailDto;
import com.mtms.finance.application.dto.TaxObligationDto;
import com.mtms.finance.application.dto.TaxObligationPageResponse;
import com.mtms.finance.domain.TaxObligationStatus;
import com.mtms.finance.domain.TaxObligationType;
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
 * P0-FIN-07 / BR-FIN-04 / TC-012 / FE-P0-15
 * REST controller for manual tax obligations and payment installments.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/finance/tax-obligations")
public class TaxObligationController {

    private final TaxObligationService taxObligationService;
    private final BranchAuthorizationService branchAuthorizationService;

    public TaxObligationController(
            TaxObligationService taxObligationService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.taxObligationService = taxObligationService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TaxObligationPageResponse> listObligations(
            @PathVariable UUID branchId,
            @RequestParam(required = false) TaxObligationType obligationType,
            @RequestParam(required = false) TaxObligationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        TaxObligationPageResponse response = taxObligationService.listObligations(
                branchId, obligationType, status, startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TaxObligationDetailDto> getObligationDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        TaxObligationDetailDto response = taxObligationService.getObligationDetail(branchId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<TaxObligationDto> createObligation(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateTaxObligationCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        TaxObligationDto response = taxObligationService.createObligation(branchId, command, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    @Idempotent(required = false)
    public ResponseEntity<TaxObligationDetailDto> payObligation(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody PayTaxObligationCommand command
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        TaxObligationDetailDto response = taxObligationService.payObligation(branchId, id, command, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TaxObligationDto> cancelObligation(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        TaxObligationDto response = taxObligationService.cancelObligation(branchId, id, currentUserId);
        return ResponseEntity.ok(response);
    }
}
