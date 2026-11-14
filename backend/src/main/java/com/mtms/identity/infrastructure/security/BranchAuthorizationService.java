package com.mtms.identity.infrastructure.security;

import com.mtms.common.error.AccessForbiddenException;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-ORG-05 / BR-ORG-04
 * Server-side branch and entity scoped authorization evaluator.
 * Registered as bean "branchAuth" for @PreAuthorize expressions and programmatic enforcement.
 */
@Service("branchAuth")
public class BranchAuthorizationService {

    private final ScopedRoleGrantJpaRepository grantRepository;

    public BranchAuthorizationService(ScopedRoleGrantJpaRepository grantRepository) {
        this.grantRepository = grantRepository;
    }

    /**
     * Checks if current authenticated user has active access to target branch.
     * SYSTEM_ADMIN role always grants access across all branches.
     */
    public boolean hasBranchAccess(UUID branchId, String requiredAction) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null || branchId == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        // 1. Check if user is active SYSTEM_ADMIN
        if (grantRepository.isSystemAdmin(currentUserId, today)) {
            return true;
        }

        // 2. Check if user has active grant for the specific branch
        List<ScopedRoleGrantJpaEntity> activeGrants =
                grantRepository.findActiveGrantsByUserIdAndBranchId(currentUserId, branchId, today);

        if (activeGrants.isEmpty()) {
            return false;
        }

        // 3. If no specific action required or action matches wildcard
        if (requiredAction == null || requiredAction.isBlank() || "*".equals(requiredAction.trim()) || "all".equalsIgnoreCase(requiredAction.trim())) {
            return true;
        }

        String targetAction = requiredAction.trim().toLowerCase();
        for (ScopedRoleGrantJpaEntity grant : activeGrants) {
            String actionScope = grant.getActionScope();
            if (actionScope == null || "*".equals(actionScope.trim()) || "all".equalsIgnoreCase(actionScope.trim())) {
                return true;
            }
            for (String allowed : actionScope.toLowerCase().split(",")) {
                String trimmed = allowed.trim();
                if (trimmed.equals(targetAction) || "*".equals(trimmed) || "all".equals(trimmed)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Enforces branch access; throws AccessForbiddenException (HTTP 403) if denied.
     */
    public void checkBranchAccess(UUID branchId, String requiredAction) {
        if (!hasBranchAccess(branchId, requiredAction)) {
            throw new AccessForbiddenException("Access to the requested branch resource is forbidden");
        }
    }

    /**
     * Checks if current authenticated user has active access to target business entity.
     */
    public boolean hasEntityAccess(UUID entityId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null || entityId == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (grantRepository.isSystemAdmin(currentUserId, today)) {
            return true;
        }

        List<ScopedRoleGrantJpaEntity> grants = grantRepository.findActiveGrantsByUserId(currentUserId, today);
        return grants.stream().anyMatch(g -> entityId.equals(g.getBusinessEntityId()));
    }

    public void checkEntityAccess(UUID entityId) {
        if (!hasEntityAccess(entityId)) {
            throw new AccessForbiddenException("Access to the requested business entity is forbidden");
        }
    }

    /**
     * Checks if current authenticated user is an active SYSTEM_ADMIN.
     */
    public boolean isSystemAdmin() {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        return grantRepository.isSystemAdmin(currentUserId, LocalDate.now());
    }

    public void checkSystemAdmin() {
        if (!isSystemAdmin()) {
            throw new AccessForbiddenException("System administrator privileges required");
        }
    }

    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        return null;
    }
}
