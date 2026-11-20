package com.mtms.inventory.web;

import com.mtms.inventory.application.dto.CogsReportDto;
import com.mtms.inventory.application.dto.InventoryDashboardDto;
import com.mtms.inventory.application.dto.StockMovementPageResponse;
import com.mtms.inventory.application.service.InventoryReportingService;
import com.mtms.inventory.domain.StockMovementType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-INV-06 / P0-INV-07 / BR-INV-01 / BR-INV-02 / TC-020 / TC-021 / FE-P0-28
 * REST Controller for branch inventory dashboard, movements audit ledger,
 * and periodic Inventory-to-COGS read model reconciliation.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/inventory")
public class InventoryReportingController {

    private final InventoryReportingService inventoryReportingService;

    public InventoryReportingController(InventoryReportingService inventoryReportingService) {
        this.inventoryReportingService = inventoryReportingService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<InventoryDashboardDto> getDashboard(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        InventoryDashboardDto dto = inventoryReportingService.getDashboard(branchId, startDate, endDate);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/movements")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockMovementPageResponse> getMovementsLedger(
            @PathVariable UUID branchId,
            @RequestParam(required = false) StockMovementType movementType,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = {"businessDate", "createdAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        StockMovementPageResponse response = inventoryReportingService.getMovementsLedger(
                branchId, movementType, itemId, startDate, endDate, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cogs-report")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<CogsReportDto> getCogsReport(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String month
    ) {
        CogsReportDto dto = inventoryReportingService.getCogsReport(branchId, startDate, endDate, month);
        return ResponseEntity.ok(dto);
    }
}
