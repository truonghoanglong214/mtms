package com.mtms.audit.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * P0-SEC-02 / BR-SEC-03 / TC-041
 * Command to record an immutable audit event.
 */
public record RecordAuditEventCommand(
        UUID branchId,
        UUID actorUserId,
        UUID correlationId,
        @NotBlank(message = "targetType is required")
        @Size(max = 80, message = "targetType must not exceed 80 characters")
        String targetType,
        @NotNull(message = "targetId is required")
        UUID targetId,
        @NotBlank(message = "eventName is required")
        @Size(max = 90, message = "eventName must not exceed 90 characters")
        String eventName,
        String actionReason,
        String oldSnapshot,
        String newSnapshot
) {
    public static RecordAuditEventCommand of(
            UUID branchId,
            UUID actorUserId,
            String targetType,
            UUID targetId,
            String eventName,
            String actionReason,
            String oldSnapshot,
            String newSnapshot
    ) {
        return new RecordAuditEventCommand(
                branchId,
                actorUserId,
                UUID.randomUUID(),
                targetType,
                targetId,
                eventName,
                actionReason,
                oldSnapshot,
                newSnapshot
        );
    }
}
