package com.mtms.organization.application.exception;

import java.util.List;
import java.util.UUID;

/**
 * P0-ORG-07 / TC-003
 * Thrown when a branch activation is requested before passing all go-live readiness prerequisites.
 */
public class BranchNotReadyException extends RuntimeException {

    private final UUID branchId;
    private final List<String> missingRequirements;

    public BranchNotReadyException(UUID branchId, List<String> missingRequirements) {
        super("Branch '" + branchId + "' is not ready for activation: " +
                (missingRequirements != null && !missingRequirements.isEmpty()
                        ? String.join("; ", missingRequirements)
                        : "Missing prerequisites"));
        this.branchId = branchId;
        this.missingRequirements = missingRequirements != null ? List.copyOf(missingRequirements) : List.of();
    }

    public UUID getBranchId() {
        return branchId;
    }

    public List<String> getMissingRequirements() {
        return missingRequirements;
    }
}
