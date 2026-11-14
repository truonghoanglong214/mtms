package com.mtms.identity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-ORG-05 / BR-ORG-04
 * Domain entity representing an authorized role grant scoped to a business entity and branch.
 */
public class ScopedRoleGrant {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final UUID userId;
    private final RoleKey roleKey;
    private final String actionScope;
    private final String rowScope;
    private final String reportPermissionGroup;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final UUID grantedBy;
    private final Instant createdAt;

    public ScopedRoleGrant(UUID id,
                           UUID businessEntityId,
                           UUID branchId,
                           UUID userId,
                           RoleKey roleKey,
                           String actionScope,
                           String rowScope,
                           String reportPermissionGroup,
                           LocalDate validFrom,
                           LocalDate validTo,
                           UUID grantedBy,
                           Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Grant ID must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "Business entity ID must not be null");
        this.userId = Objects.requireNonNull(userId, "User ID must not be null");
        this.roleKey = Objects.requireNonNull(roleKey, "Role key must not be null");
        this.actionScope = (actionScope != null && !actionScope.isBlank()) ? actionScope.trim() : "*";
        this.rowScope = rowScope;
        this.reportPermissionGroup = reportPermissionGroup;
        this.validFrom = Objects.requireNonNull(validFrom, "Valid from date must not be null");
        this.validTo = validTo;
        this.branchId = branchId;
        this.grantedBy = Objects.requireNonNull(grantedBy, "Granted by user ID must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();

        validateInvariants();
    }

    private void validateInvariants() {
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("Valid to date (" + validTo + ") cannot be before valid from date (" + validFrom + ")");
        }
        if (roleKey != RoleKey.SYSTEM_ADMIN && branchId == null) {
            throw new IllegalArgumentException("Branch ID is required for non-system admin role: " + roleKey);
        }
    }

    public static ScopedRoleGrant create(UUID businessEntityId,
                                         UUID branchId,
                                         UUID userId,
                                         RoleKey roleKey,
                                         String actionScope,
                                         String rowScope,
                                         String reportPermissionGroup,
                                         LocalDate validFrom,
                                         LocalDate validTo,
                                         UUID grantedBy) {
        return new ScopedRoleGrant(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                userId,
                roleKey,
                actionScope,
                rowScope,
                reportPermissionGroup,
                validFrom != null ? validFrom : LocalDate.now(),
                validTo,
                grantedBy,
                Instant.now()
        );
    }

    /**
     * Checks if this role grant is active on a specific business date.
     */
    public boolean isActiveOn(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        boolean afterStart = !date.isBefore(validFrom);
        boolean beforeEnd = (validTo == null) || !date.isAfter(validTo);
        return afterStart && beforeEnd;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getUserId() {
        return userId;
    }

    public RoleKey getRoleKey() {
        return roleKey;
    }

    public String getActionScope() {
        return actionScope;
    }

    public String getRowScope() {
        return rowScope;
    }

    public String getReportPermissionGroup() {
        return reportPermissionGroup;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScopedRoleGrant that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
