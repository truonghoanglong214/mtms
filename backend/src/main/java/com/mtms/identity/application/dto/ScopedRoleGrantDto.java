package com.mtms.identity.application.dto;

import com.mtms.identity.domain.RoleKey;
import com.mtms.identity.domain.ScopedRoleGrant;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScopedRoleGrantDto(
        UUID id,
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
        Instant createdAt,
        boolean active
) {
    public static ScopedRoleGrantDto fromDomain(ScopedRoleGrant domain) {
        return new ScopedRoleGrantDto(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getUserId(),
                domain.getRoleKey(),
                domain.getActionScope(),
                domain.getRowScope(),
                domain.getReportPermissionGroup(),
                domain.getValidFrom(),
                domain.getValidTo(),
                domain.getGrantedBy(),
                domain.getCreatedAt(),
                domain.isActiveOn(LocalDate.now())
        );
    }
}
