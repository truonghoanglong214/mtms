package com.mtms.identity.web;

import com.mtms.common.api.ApiResponse;
import com.mtms.identity.application.ScopedPermissionService;
import com.mtms.identity.application.dto.GrantRoleCommand;
import com.mtms.identity.application.dto.ScopedRoleGrantDto;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * P0-ORG-05 / BR-ORG-04
 * REST Controller for managing branch-scoped role grants.
 * Only SYSTEM_ADMIN or authorized owners can grant or revoke roles.
 */
@RestController
@RequestMapping("/api/v1/auth/grants")
public class ScopedRoleGrantController {

    private final ScopedPermissionService scopedPermissionService;
    private final BranchAuthorizationService branchAuthorizationService;

    public ScopedRoleGrantController(ScopedPermissionService scopedPermissionService,
                                     BranchAuthorizationService branchAuthorizationService) {
        this.scopedPermissionService = scopedPermissionService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @PostMapping
    @PreAuthorize("@branchAuth.isSystemAdmin()")
    public ResponseEntity<ApiResponse<ScopedRoleGrantDto>> grantRole(
            @Valid @RequestBody GrantRoleCommand command) {
        UUID actorId = branchAuthorizationService.getCurrentUserId();
        if (actorId == null) {
            actorId = command.userId(); // fallback for system initialization
        }
        ScopedRoleGrantDto granted = scopedPermissionService.grantRole(command, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(granted, "Role granted successfully"));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@branchAuth.isSystemAdmin() or @branchAuth.getCurrentUserId() == #userId")
    public ResponseEntity<ApiResponse<List<ScopedRoleGrantDto>>> getUserGrants(@PathVariable UUID userId) {
        List<ScopedRoleGrantDto> grants = scopedPermissionService.getUserGrants(userId);
        return ResponseEntity.ok(ApiResponse.ok(grants));
    }

    @DeleteMapping("/{grantId}")
    @PreAuthorize("@branchAuth.isSystemAdmin()")
    public ResponseEntity<ApiResponse<Void>> revokeGrant(@PathVariable UUID grantId) {
        scopedPermissionService.revokeGrant(grantId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role grant revoked successfully"));
    }
}
