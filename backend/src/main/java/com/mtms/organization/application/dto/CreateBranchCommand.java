package com.mtms.organization.application.dto;

import com.mtms.organization.domain.BranchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBranchCommand(
        @NotNull(message = "Business entity ID must not be null")
        UUID businessEntityId,

        @NotBlank(message = "Branch code must not be blank")
        @Size(max = 40, message = "Branch code must not exceed 40 characters")
        String branchCode,

        @NotBlank(message = "Branch name must not be blank")
        @Size(max = 160, message = "Branch name must not exceed 160 characters")
        String name,

        @Size(max = 64, message = "Time zone must not exceed 64 characters")
        String timeZone,

        @NotNull(message = "Branch status must be specified")
        BranchStatus status
) {
}
