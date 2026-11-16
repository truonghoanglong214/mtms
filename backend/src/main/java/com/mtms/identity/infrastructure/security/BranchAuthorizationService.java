package com.mtms.identity.infrastructure.security;

import com.mtms.common.error.AccessForbiddenException;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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

    private static final Set<String> ROLE_KEYWORDS = Set.of(
            "ADMIN", "SYSTEM_ADMIN", "OWNER", "BRANCH_OWNER", "MANAGER", "FINANCE", "EMPLOYEE", "INVESTOR"
    );

    /**
     * Checks if current authenticated user has active access to target branch.
     * Evaluates against roles (e.g. 'ADMIN', 'OWNER', 'MANAGER') or actions (e.g. 'READ', 'WRITE').
     * SYSTEM_ADMIN role always grants access across all branches.
     */
    public boolean hasBranchAccess(UUID branchId, String... rolesOrActions) {
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

        // 3. If no specific action or role required
        if (rolesOrActions == null || rolesOrActions.length == 0) {
            return true;
        }

        for (String item : rolesOrActions) {
            if (item == null || item.isBlank() || "*".equals(item.trim()) || "all".equalsIgnoreCase(item.trim())) {
                return true;
            }
            String target = item.trim().toUpperCase();
            if (ROLE_KEYWORDS.contains(target)) {
                for (ScopedRoleGrantJpaEntity grant : activeGrants) {
                    if (matchesRole(grant.getRoleKey(), target)) {
                        return true;
                    }
                }
            } else {
                for (ScopedRoleGrantJpaEntity grant : activeGrants) {
                    String actionScope = grant.getActionScope();
                    if (actionScope == null || "*".equals(actionScope.trim()) || "all".equalsIgnoreCase(actionScope.trim())) {
                        return true;
                    }
                    for (String allowed : actionScope.toUpperCase().split(",")) {
                        String trimmed = allowed.trim();
                        if (trimmed.equals(target) || "*".equals(trimmed) || "ALL".equals(trimmed)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Alias for hasBranchAccess to support hasBranchPermission syntax.
     */
    public boolean hasBranchPermission(UUID branchId, String... rolesOrActions) {
        return hasBranchAccess(branchId, rolesOrActions);
    }

    private boolean matchesRole(String actualRoleKey, String targetRole) {
        if (actualRoleKey == null) {
            return false;
        }
        String actual = actualRoleKey.trim().toUpperCase();
        if (actual.equals(targetRole)) {
            return true;
        }
        if ("ADMIN".equals(targetRole) && ("SYSTEM_ADMIN".equals(actual) || actual.contains("ADMIN"))) {
            return true;
        }
        if ("OWNER".equals(targetRole) && ("BRANCH_OWNER".equals(actual) || actual.contains("OWNER"))) {
            return true;
        }
        if ("MANAGER".equals(targetRole) && ("BRANCH_OWNER".equals(actual) || actual.contains("MANAGER") || actual.contains("OWNER"))) {
            return true;
        }
        return false;
    }

    /**
     * Enforces branch access; throws AccessForbiddenException (HTTP 403) if denied.
     */
    public void checkBranchAccess(UUID branchId, String... rolesOrActions) {
        if (!hasBranchAccess(branchId, rolesOrActions)) {
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
