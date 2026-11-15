package com.mtms.evidence.web;

import com.mtms.common.api.ApiResponse;
import com.mtms.common.error.UnauthorizedException;
import com.mtms.evidence.application.EvidenceService;
import com.mtms.evidence.application.dto.DownloadTokenResponse;
import com.mtms.evidence.application.dto.EvidenceFileDto;
import com.mtms.evidence.application.dto.EvidenceUploadResponse;
import com.mtms.evidence.application.dto.FilePayload;
import com.mtms.evidence.domain.VisibilityClass;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
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
 * P0-SEC-01 / BR-SEC-01 / TC-043
 * REST controller for branch evidence file management, time-limited token issuance,
 * and authenticated streaming.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Evidence", description = "Endpoints for private evidence storage and branch-bounded token access")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final BranchAuthorizationService branchAuthorizationService;

    public EvidenceController(
            EvidenceService evidenceService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.evidenceService = evidenceService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @PostMapping(value = "/branches/{branchId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER', 'MANAGER', 'FINANCE', 'EMPLOYEE')")
    @Operation(summary = "Upload evidence file with size and MIME validation")
    public ResponseEntity<ApiResponse<EvidenceUploadResponse>> uploadEvidence(
            @PathVariable UUID branchId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "visibility", required = false, defaultValue = "BRANCH_PRIVATE") VisibilityClass visibility
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        EvidenceUploadResponse response = evidenceService.uploadEvidence(branchId, file, visibility, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Evidence file uploaded successfully"));
    }

    @GetMapping("/branches/{branchId}/evidence")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER', 'MANAGER', 'FINANCE')")
    @Operation(summary = "List evidence files for a branch")
    public ResponseEntity<ApiResponse<List<EvidenceFileDto>>> listEvidence(
            @PathVariable UUID branchId
    ) {
        List<EvidenceFileDto> files = evidenceService.listEvidenceByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @GetMapping("/evidence/{fileId}")
    @Operation(summary = "Get evidence file metadata by ID")
    public ResponseEntity<ApiResponse<EvidenceFileDto>> getEvidence(
            @PathVariable UUID fileId
    ) {
        EvidenceFileDto file = evidenceService.getEvidenceFile(fileId);
        branchAuthorizationService.checkBranchAccess(file.branchId(), "ADMIN", "OWNER", "MANAGER", "FINANCE", "EMPLOYEE");
        return ResponseEntity.ok(ApiResponse.ok(file));
    }

    @PostMapping("/evidence/{fileId}/token")
    @Operation(summary = "Generate a 15-minute time-limited HMAC-SHA256 signed download token (TC-043)")
    public ResponseEntity<ApiResponse<DownloadTokenResponse>> generateToken(
            @PathVariable UUID fileId
    ) {
        UUID currentUserId = branchAuthorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        DownloadTokenResponse response = evidenceService.generateDownloadToken(fileId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Download token generated successfully"));
    }

    @GetMapping("/evidence/{fileId}/content")
    @Operation(summary = "Stream evidence file content using time-limited signed token (no auth header required)")
    public ResponseEntity<InputStreamResource> downloadContent(
            @PathVariable UUID fileId,
            @RequestParam("token") String token
    ) {
        FilePayload payload = evidenceService.loadContentStream(fileId, token);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .contentLength(payload.byteSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payload.originalFilename() + "\"")
                .body(new InputStreamResource(payload.inputStream()));
    }
}
