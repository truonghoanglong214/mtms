package com.mtms.inventory.web;

import com.mtms.inventory.application.dto.BranchPayablesResponse;
import com.mtms.inventory.application.service.PayableService;
import com.mtms.inventory.domain.PayableStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * P0-INV-03 / BR-INV-05 / TC-008
 * REST Controller for branch supplier payables ledger and outstanding debt inspection.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/inventory/payables")
public class PayableController {

    private final PayableService payableService;

    public PayableController(PayableService payableService) {
        this.payableService = payableService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<BranchPayablesResponse> getPayables(
            @PathVariable UUID branchId,
            @RequestParam(required = false) PayableStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        BranchPayablesResponse response = payableService.getBranchPayables(branchId, status, search, pageable);
        return ResponseEntity.ok(response);
    }
}
