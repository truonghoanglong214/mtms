package com.mtms.inventory.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.inventory.application.dto.CommitOpeningCommand;
import com.mtms.inventory.application.dto.OpeningInventoryRecordDto;
import com.mtms.inventory.application.dto.OpeningInventoryStatusResponse;
import com.mtms.inventory.application.dto.OpeningPreviewResponse;
import com.mtms.inventory.application.dto.PreviewOpeningCommand;
import com.mtms.inventory.application.service.OpeningInventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * P0-INV-02 / BR-INV-08 / TC-024
 * REST Controller for branch opening inventory management, preview, and post-once commit.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/inventory/opening")
public class OpeningInventoryController {

    private final OpeningInventoryService openingInventoryService;
    private final BranchAuthorizationService branchAuthorizationService;

    public OpeningInventoryController(
            OpeningInventoryService openingInventoryService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.openingInventoryService = openingInventoryService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<OpeningInventoryStatusResponse> getOpeningStatus(@PathVariable UUID branchId) {
        OpeningInventoryStatusResponse status = openingInventoryService.getOpeningStatus(branchId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/preview")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')")
    public ResponseEntity<OpeningPreviewResponse> previewOpening(
            @PathVariable UUID branchId,
            @Valid @RequestBody PreviewOpeningCommand command
    ) {
        OpeningPreviewResponse preview = openingInventoryService.previewOpening(branchId, command);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/commit")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')")
    @Idempotent(required = false)
    public ResponseEntity<OpeningInventoryRecordDto> commitOpening(
            @PathVariable UUID branchId,
            @Valid @RequestBody CommitOpeningCommand command
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        OpeningInventoryRecordDto committed = openingInventoryService.commitOpening(branchId, command, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(committed);
    }
}
