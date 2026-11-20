package com.mtms.stockcount.web;

import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.stockcount.application.StockCountService;
import com.mtms.stockcount.application.dto.StockCountDtos.CreateStockCountRequest;
import com.mtms.stockcount.application.dto.StockCountDtos.StockCountDetailDto;
import com.mtms.stockcount.application.dto.StockCountDtos.StockCountListResponse;
import com.mtms.stockcount.application.dto.StockCountDtos.StockCountSummaryDto;
import com.mtms.stockcount.application.dto.StockCountDtos.UpdateStockCountRequest;
import com.mtms.stockcount.domain.model.StockCountStatus;
import com.mtms.stockcount.domain.model.StockCountType;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/stock-counts")
public class StockCountController {

    private final StockCountService stockCountService;
    private final BranchAuthorizationService branchAuthorizationService;

    public StockCountController(
            StockCountService stockCountService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.stockCountService = stockCountService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockCountDetailDto> createStockCount(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateStockCountRequest request
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        StockCountDetailDto response = stockCountService.createStockCount(branchId, request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{countId}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockCountDetailDto> updateCounts(
            @PathVariable UUID branchId,
            @PathVariable UUID countId,
            @Valid @RequestBody UpdateStockCountRequest request
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        StockCountDetailDto response = stockCountService.updateCounts(branchId, countId, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{countId}/submit")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockCountDetailDto> submitStockCount(
            @PathVariable UUID branchId,
            @PathVariable UUID countId
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        StockCountDetailDto response = stockCountService.submitStockCount(branchId, countId, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{countId}/approve")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockCountDetailDto> approveAndPostStockCount(
            @PathVariable UUID branchId,
            @PathVariable UUID countId
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        StockCountDetailDto response = stockCountService.approveAndPostStockCount(branchId, countId, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{countId}/cancel")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<Void> cancelStockCount(
            @PathVariable UUID branchId,
            @PathVariable UUID countId
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        stockCountService.cancelStockCount(branchId, countId, actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockCountListResponse> getStockCounts(
            @PathVariable UUID branchId,
            @RequestParam(required = false) StockCountStatus status,
            @RequestParam(required = false) StockCountType countType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        StockCountListResponse response = stockCountService.getStockCounts(
                branchId, status, countType, startDate, endDate, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{countId}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockCountDetailDto> getStockCountDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID countId
    ) {
        StockCountDetailDto response = stockCountService.getStockCountDetail(branchId, countId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockCountSummaryDto> getStockCountSummary(
            @PathVariable UUID branchId
    ) {
        StockCountSummaryDto response = stockCountService.getStockCountSummary(branchId);
        return ResponseEntity.ok(response);
    }
}
