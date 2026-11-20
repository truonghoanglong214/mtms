package com.mtms.movement.web;

import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.movement.application.TransferService;
import com.mtms.movement.application.dto.TransferDtos.CreateTransferRequest;
import com.mtms.movement.application.dto.TransferDtos.ReceiveTransferRequest;
import com.mtms.movement.application.dto.TransferDtos.TransferDetailDto;
import com.mtms.movement.application.dto.TransferDtos.TransferListResponse;
import com.mtms.movement.application.dto.TransferDtos.TransferSummaryDto;
import com.mtms.movement.domain.model.TransferStatus;
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
@RequestMapping("/api/v1/branches/{branchId}/transfers")
public class TransferController {

    private final TransferService transferService;
    private final BranchAuthorizationService branchAuthorizationService;

    public TransferController(
            TransferService transferService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.transferService = transferService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TransferDetailDto> createTransfer(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        TransferDetailDto response = transferService.createTransfer(branchId, request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{transferId}/dispatch")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TransferDetailDto> dispatchTransfer(
            @PathVariable UUID branchId,
            @PathVariable UUID transferId
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        TransferDetailDto response = transferService.dispatchTransfer(branchId, transferId, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{transferId}/receive")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TransferDetailDto> receiveTransfer(
            @PathVariable UUID branchId,
            @PathVariable UUID transferId,
            @Valid @RequestBody ReceiveTransferRequest request
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        TransferDetailDto response = transferService.receiveTransfer(branchId, transferId, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{transferId}/cancel")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<Void> cancelTransfer(
            @PathVariable UUID branchId,
            @PathVariable UUID transferId
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        transferService.cancelTransfer(branchId, transferId, actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/outbound")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TransferListResponse> getOutboundTransfers(
            @PathVariable UUID branchId,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        TransferListResponse response = transferService.getOutboundTransfers(
                branchId, status, startDate, endDate, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inbound")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TransferListResponse> getInboundTransfers(
            @PathVariable UUID branchId,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        TransferListResponse response = transferService.getInboundTransfers(
                branchId, status, startDate, endDate, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transferId}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TransferDetailDto> getTransferDetail(
            @PathVariable UUID branchId,
            @PathVariable UUID transferId
    ) {
        TransferDetailDto response = transferService.getTransferDetail(branchId, transferId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<TransferSummaryDto> getTransferSummary(
            @PathVariable UUID branchId
    ) {
        TransferSummaryDto response = transferService.getTransferSummary(branchId);
        return ResponseEntity.ok(response);
    }
}
