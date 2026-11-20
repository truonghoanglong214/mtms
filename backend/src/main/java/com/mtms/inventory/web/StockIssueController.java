package com.mtms.inventory.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.inventory.application.dto.CreateStockIssueCommand;
import com.mtms.inventory.application.dto.StockDocumentDetailDto;
import com.mtms.inventory.application.dto.StockDocumentListResponse;
import com.mtms.inventory.application.service.StockIssueService;
import com.mtms.inventory.domain.StockDocumentStatus;
import com.mtms.inventory.domain.StockDocumentType;
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
 * P0-INV-04 / P0-INV-05 / BR-INV-02 / BR-INV-03
 * REST Controller for branch manual consumption issues and moving WAC postings.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/inventory/issues")
public class StockIssueController {

    private final StockIssueService stockIssueService;
    private final BranchAuthorizationService branchAuthorizationService;

    public StockIssueController(
            StockIssueService stockIssueService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.stockIssueService = stockIssueService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockDocumentListResponse> getIssueDocuments(
            @PathVariable UUID branchId,
            @RequestParam(required = false) StockDocumentStatus status,
            @RequestParam(required = false) StockDocumentType documentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        StockDocumentListResponse response = stockIssueService.getIssueDocuments(
                branchId, status, documentType, startDate, endDate, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER', 'EMPLOYEE')")
    @Idempotent(required = false)
    public ResponseEntity<StockDocumentDetailDto> createIssue(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateStockIssueCommand command
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        StockDocumentDetailDto created = stockIssueService.createIssueDocument(branchId, command, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<StockDocumentDetailDto> getIssueDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID documentId
    ) {
        StockDocumentDetailDto detail = stockIssueService.getIssueDocumentById(branchId, documentId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{documentId}/post")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')")
    @Idempotent(required = false)
    public ResponseEntity<StockDocumentDetailDto> postIssue(
            @PathVariable UUID branchId,
            @PathVariable UUID documentId
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        StockDocumentDetailDto posted = stockIssueService.postIssueDocument(branchId, documentId, actorUserId);
        return ResponseEntity.ok(posted);
    }
}
