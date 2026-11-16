package com.mtms.audit.application.dto;

import com.mtms.audit.domain.AuditEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * P0-SEC-02 / BR-SEC-03 / TC-041
 * Data transfer object for audit events.
 */
public record AuditEventDto(
        UUID id,
        UUID branchId,
        UUID actorUserId,
        UUID correlationId,
        String targetType,
        UUID targetId,
        String eventName,
        String actionReason,
        String oldSnapshot,
        String newSnapshot,
        Instant occurredAt
) {
    public static AuditEventDto fromDomain(AuditEvent domain) {
        if (domain == null) {
            return null;
        }
        return new AuditEventDto(
                domain.getId(),
                domain.getBranchId(),
                domain.getActorUserId(),
                domain.getCorrelationId(),
                domain.getTargetType(),
                domain.getTargetId(),
                domain.getEventName(),
                domain.getActionReason(),
                domain.getOldSnapshot(),
                domain.getNewSnapshot(),
                domain.getOccurredAt()
        );
    }
}
