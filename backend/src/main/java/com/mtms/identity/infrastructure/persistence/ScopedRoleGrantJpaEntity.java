package com.mtms.identity.infrastructure.persistence;

import com.mtms.identity.domain.RoleKey;
import com.mtms.identity.domain.ScopedRoleGrant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "scoped_role_grants", schema = "mtms")
public class ScopedRoleGrantJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_key", nullable = false, length = 40)
    private String roleKey;

    @Column(name = "action_scope", nullable = false, length = 80)
    private String actionScope;

    @Column(name = "row_scope", length = 80)
    private String rowScope;

    @Column(name = "report_permission_group", length = 50)
    private String reportPermissionGroup;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "granted_by", nullable = false)
    private UUID grantedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScopedRoleGrantJpaEntity() {
    }

    public ScopedRoleGrantJpaEntity(UUID id,
                                   UUID businessEntityId,
                                   UUID branchId,
                                   UUID userId,
                                   String roleKey,
                                   String actionScope,
                                   String rowScope,
                                   String reportPermissionGroup,
                                   LocalDate validFrom,
                                   LocalDate validTo,
                                   UUID grantedBy,
                                   Instant createdAt) {
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.userId = userId;
        this.roleKey = roleKey;
        this.actionScope = actionScope;
        this.rowScope = rowScope;
        this.reportPermissionGroup = reportPermissionGroup;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.grantedBy = grantedBy;
        this.createdAt = createdAt;
    }

    public static ScopedRoleGrantJpaEntity fromDomain(ScopedRoleGrant domain) {
        return new ScopedRoleGrantJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getUserId(),
                domain.getRoleKey().name(),
                domain.getActionScope(),
                domain.getRowScope(),
                domain.getReportPermissionGroup(),
                domain.getValidFrom(),
                domain.getValidTo(),
                domain.getGrantedBy(),
                domain.getCreatedAt()
        );
    }

    public ScopedRoleGrant toDomain() {
        return new ScopedRoleGrant(
                id,
                businessEntityId,
                branchId,
                userId,
                RoleKey.valueOf(roleKey),
                actionScope,
                rowScope,
                reportPermissionGroup,
                validFrom,
                validTo,
                grantedBy,
                createdAt
        );
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

    public String getRoleKey() {
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
}
