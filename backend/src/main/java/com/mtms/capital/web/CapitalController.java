package com.mtms.capital.web;

import com.mtms.capital.application.CapitalService;
import com.mtms.capital.application.dto.CapitalDtos.*;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CapitalController {

    private final CapitalService capitalService;
    private final BranchAuthorizationService branchAuthorizationService;

    public CapitalController(CapitalService capitalService,
                             BranchAuthorizationService branchAuthorizationService) {
        this.capitalService = capitalService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    // 1. Investor Management (Entity Scoped)
    @GetMapping("/entities/{entityId}/investors")
    @PreAuthorize("@branchAuth.hasEntityAccess(#entityId)")
    public ResponseEntity<List<InvestorDto>> getInvestorsByEntity(@PathVariable UUID entityId) {
        return ResponseEntity.ok(capitalService.getInvestorsByEntity(entityId));
    }

    @PostMapping("/entities/{entityId}/investors")
    @PreAuthorize("@branchAuth.hasEntityAccess(#entityId)")
    public ResponseEntity<InvestorDto> registerInvestor(
            @PathVariable UUID entityId,
            @Valid @RequestBody CreateInvestorRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(capitalService.registerInvestor(entityId, request));
    }

    // 2. Agreements Management (Branch Scoped)
    @GetMapping("/branches/{branchId}/capital/agreements")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<AgreementDto>> getAgreementsByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(capitalService.getAgreementsByBranch(branchId));
    }

    @PostMapping("/branches/{branchId}/capital/agreements")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<AgreementDto> createAgreement(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateAgreementRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(capitalService.createAgreement(branchId, request));
    }

    // 3. Commitments Management (Branch Scoped)
    @GetMapping("/branches/{branchId}/capital/commitments")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<CommitmentDto>> getCommitmentsByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(capitalService.getCommitmentsByBranch(branchId));
    }

    @PostMapping("/branches/{branchId}/capital/commitments")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<CommitmentDto> createCommitment(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateCommitmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(capitalService.createCommitment(branchId, request));
    }

    // 4. Contribution Recording (Branch Scoped)
    @PostMapping("/branches/{branchId}/capital/commitments/{commitmentId}/contributions")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<ContributionDto> recordContribution(
            @PathVariable UUID branchId,
            @PathVariable UUID commitmentId,
            @Valid @RequestBody RecordContributionRequest request
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(capitalService.recordContribution(branchId, commitmentId, request, actorUserId));
    }

    @GetMapping("/branches/{branchId}/capital/contributions")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<ContributionDto>> getContributionsByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(capitalService.getContributionsByBranch(branchId));
    }

    // 5. Profit Entitlement Terms (Branch Scoped, P0 Read-Only / Configured)
    @GetMapping("/branches/{branchId}/capital/profit-terms")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<ProfitTermDto>> getProfitTermsByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(capitalService.getProfitTermsByBranch(branchId));
    }

    @PostMapping("/branches/{branchId}/capital/profit-terms")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<ProfitTermDto> configureProfitTerms(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateProfitTermRequest request
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(capitalService.configureProfitTerms(branchId, request, actorUserId));
    }

    // 6. Capital Summary & TC-032 / CAP-INV-01 Reconciliation
    @GetMapping("/branches/{branchId}/capital/summary")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<CapitalSummaryDto> getCapitalSummary(@PathVariable UUID branchId) {
        return ResponseEntity.ok(capitalService.getCapitalSummary(branchId));
    }
}
