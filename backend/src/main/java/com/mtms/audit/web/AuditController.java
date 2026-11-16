package com.mtms.audit.web;

import com.mtms.audit.application.AuditLogService;
import com.mtms.audit.application.dto.AuditEventDto;
import com.mtms.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * P0-SEC-02 / BR-SEC-03 / TC-041
 * REST controller for immutable activity log & audit event spine queries.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Audit Log", description = "Endpoints for immutable activity audit trail inspection")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-events")
    @PreAuthorize("@branchAuth.isSystemAdmin()")
    @Operation(summary = "Query system-wide global activity audit events (System Admin only)")
    public ResponseEntity<ApiResponse<Page<AuditEventDto>>> getGlobalAuditEvents(
            @PageableDefault(size = 50, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditEventDto> events = auditLogService.listGlobalEvents(pageable);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/branches/{branchId}/audit-events")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER', 'MANAGER')")
    @Operation(summary = "Query branch-scoped activity audit events (Branch ADMIN/OWNER/MANAGER)")
    public ResponseEntity<ApiResponse<Page<AuditEventDto>>> getBranchAuditEvents(
            @PathVariable UUID branchId,
            @PageableDefault(size = 50, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditEventDto> events = auditLogService.listEventsByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/audit-events/target")
    @PreAuthorize("@branchAuth.isSystemAdmin()")
    @Operation(summary = "Query audit events by target entity type and ID")
    public ResponseEntity<ApiResponse<List<AuditEventDto>>> getAuditEventsByTarget(
            @RequestParam("targetType") String targetType,
            @RequestParam("targetId") UUID targetId
    ) {
        List<AuditEventDto> events = auditLogService.findByTarget(targetType, targetId);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }
}
