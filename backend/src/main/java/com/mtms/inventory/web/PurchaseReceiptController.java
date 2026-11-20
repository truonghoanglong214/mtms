package com.mtms.inventory.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.inventory.application.dto.CreatePurchaseReceiptCommand;
import com.mtms.inventory.application.dto.PurchaseReceiptDetailDto;
import com.mtms.inventory.application.dto.PurchaseReceiptDto;
import com.mtms.inventory.application.dto.PurchaseReceiptListResponse;
import com.mtms.inventory.application.service.PurchaseReceiptService;
import com.mtms.inventory.domain.ReceiptStatus;
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
 * P0-INV-03 / BR-INV-05 / TC-008
 * REST Controller for branch supplier goods receipt management and posting.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/inventory/receipts")
public class PurchaseReceiptController {

    private final PurchaseReceiptService purchaseReceiptService;
    private final BranchAuthorizationService branchAuthorizationService;

    public PurchaseReceiptController(
            PurchaseReceiptService purchaseReceiptService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.purchaseReceiptService = purchaseReceiptService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<PurchaseReceiptListResponse> getReceipts(
            @PathVariable UUID branchId,
            @RequestParam(required = false) ReceiptStatus status,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        PurchaseReceiptListResponse response = purchaseReceiptService.getReceipts(
                branchId, status, supplierId, startDate, endDate, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')")
    @Idempotent(required = false)
    public ResponseEntity<PurchaseReceiptDto> createReceipt(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreatePurchaseReceiptCommand command
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        PurchaseReceiptDto created = purchaseReceiptService.createReceipt(branchId, command, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{receiptId}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<PurchaseReceiptDetailDto> getReceiptDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID receiptId
    ) {
        PurchaseReceiptDetailDto detail = purchaseReceiptService.getReceiptById(branchId, receiptId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{receiptId}/post")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')")
    @Idempotent(required = false)
    public ResponseEntity<PurchaseReceiptDto> postReceipt(
            @PathVariable UUID branchId,
            @PathVariable UUID receiptId
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        PurchaseReceiptDto posted = purchaseReceiptService.postReceipt(branchId, receiptId, actorUserId);
        return ResponseEntity.ok(posted);
    }
}
