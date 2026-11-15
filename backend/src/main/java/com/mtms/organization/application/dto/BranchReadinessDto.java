package com.mtms.organization.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * P0-ORG-07 / BR-ORG-03 / TC-003
 * Operational readiness state for branch go-live activation.
 */
public record BranchReadinessDto(
        UUID branchId,
        boolean readyForActivation,
        List<String> missingRequirements
) {
}
