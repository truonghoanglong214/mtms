package com.mtms.identity.application.dto;

import com.mtms.identity.domain.RoleKey;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record GrantRoleCommand(
        @NotNull(message = "Business entity ID is required")
        UUID businessEntityId,

        UUID branchId,

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Role key is required")
        RoleKey roleKey,

        String actionScope,
        String rowScope,
        String reportPermissionGroup,
        LocalDate validFrom,
        LocalDate validTo
) {
}
