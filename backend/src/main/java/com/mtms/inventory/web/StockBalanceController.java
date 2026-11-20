package com.mtms.inventory.web;

import com.mtms.inventory.application.dto.StockBalanceResponse;
import com.mtms.inventory.application.service.StockBalanceService;
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
 * P0-INV-02 / BR-INV-08
 * REST Controller for branch on-hand stock balances and valuation query.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/inventory/balances")
public class StockBalanceController {

    private final StockBalanceService stockBalanceService;

    public StockBalanceController(StockBalanceService stockBalanceService) {
        this.stockBalanceService = stockBalanceService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockBalanceResponse> getBranchBalances(
            @PathVariable UUID branchId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        StockBalanceResponse response = stockBalanceService.getBranchBalances(branchId, search, pageable);
        return ResponseEntity.ok(response);
    }
}
