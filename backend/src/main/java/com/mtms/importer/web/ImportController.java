package com.mtms.importer.web;

import com.mtms.common.api.ApiResponse;
import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.importer.application.ImportStagingService;
import com.mtms.importer.application.dto.ImportBatchDto;
import com.mtms.importer.application.dto.ImportRowDto;
import com.mtms.importer.domain.ImportKind;
import com.mtms.importer.domain.ImportRowStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02 / TC-045
 * REST Controller for Excel and CSV Import Staging and Batch Commit.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/imports")
@Tag(name = "Import Staging", description = "Endpoints for staging, validating, and committing Excel & CSV imports")
public class ImportController {

    private final ImportStagingService importStagingService;
    private final BranchAuthorizationService branchAuthorizationService;

    public ImportController(
            ImportStagingService importStagingService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.importStagingService = importStagingService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @PostMapping(value = "/stage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@branchAuth.hasBranchPermission(#branchId, 'SYSTEM_ADMIN') or @branchAuth.hasBranchPermission(#branchId, 'MANAGER')")
    @Operation(summary = "Stage an Excel or CSV file for preview, formula sanitization, and row-level validation")
    public ResponseEntity<ApiResponse<ImportBatchDto>> stageFile(
            @PathVariable("branchId") UUID branchId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("importKind") ImportKind importKind
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        ImportBatchDto batch = importStagingService.stageFile(branchId, importKind, file, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(batch, "Import file staged and validated successfully"));
    }

    @GetMapping("/{batchId}")
    @PreAuthorize("@branchAuth.hasBranchPermission(#branchId, 'SYSTEM_ADMIN') or @branchAuth.hasBranchPermission(#branchId, 'MANAGER')")
    @Operation(summary = "Get details and summary statistics of an import batch")
    public ResponseEntity<ApiResponse<ImportBatchDto>> getBatch(
            @PathVariable("branchId") UUID branchId,
            @PathVariable("batchId") UUID batchId
    ) {
        ImportBatchDto batch = importStagingService.getBatch(branchId, batchId);
        return ResponseEntity.ok(ApiResponse.ok(batch));
    }

    @GetMapping("/{batchId}/rows")
    @PreAuthorize("@branchAuth.hasBranchPermission(#branchId, 'SYSTEM_ADMIN') or @branchAuth.hasBranchPermission(#branchId, 'MANAGER')")
    @Operation(summary = "Get staged rows for an import batch, with optional status filter (e.g. ERROR, VALID)")
    public ResponseEntity<ApiResponse<List<ImportRowDto>>> getBatchRows(
            @PathVariable("branchId") UUID branchId,
            @PathVariable("batchId") UUID batchId,
            @RequestParam(value = "status", required = false) ImportRowStatus status
    ) {
        List<ImportRowDto> rows = importStagingService.getBatchRows(branchId, batchId, status);
        return ResponseEntity.ok(ApiResponse.ok(rows));
    }

    @Idempotent
    @PostMapping("/{batchId}/commit")
    @PreAuthorize("@branchAuth.hasBranchPermission(#branchId, 'SYSTEM_ADMIN') or @branchAuth.hasBranchPermission(#branchId, 'MANAGER')")
    @Operation(summary = "Commit a validated import batch; idempotent and guarded against duplicate execution")
    public ResponseEntity<ApiResponse<ImportBatchDto>> commitBatch(
            @PathVariable("branchId") UUID branchId,
            @PathVariable("batchId") UUID batchId
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        ImportBatchDto committed = importStagingService.commitBatch(branchId, batchId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(committed, "Import batch committed successfully"));
    }
}
