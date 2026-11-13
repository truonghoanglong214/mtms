package com.mtms.organization.application.dto;

import com.mtms.organization.domain.BranchStatus;

import java.time.Instant;
import java.util.UUID;

public record BranchDto(
        UUID id,
        UUID businessEntityId,
        String branchCode,
        String name,
        String timeZone,
        BranchStatus status,
        Instant createdAt
) {
}
