package com.mtms.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-SEC-02 / BR-SEC-03 / TC-041
 * Immutable domain model representing an audit event in the system activity log.
 */
public class AuditEvent {

    private final UUID id;
    private final UUID branchId;
    private final UUID actorUserId;
    private final UUID correlationId;
    private final String targetType;
    private final UUID targetId;
    private final String eventName;
    private final String actionReason;
    private final String oldSnapshot;
    private final String newSnapshot;
    private final Instant occurredAt;

    public AuditEvent(
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
        this.id = Objects.requireNonNull(id, "Audit event ID must not be null");
        this.branchId = branchId;
        this.actorUserId = actorUserId;
        this.correlationId = correlationId;
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.eventName = Objects.requireNonNull(eventName, "eventName must not be null");
        this.actionReason = actionReason;
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public static AuditEvent create(
            UUID branchId,
            UUID actorUserId,
            UUID correlationId,
            String targetType,
            UUID targetId,
            String eventName,
            String actionReason,
            String oldSnapshot,
            String newSnapshot
    ) {
        return new AuditEvent(
                UUID.randomUUID(),
                branchId,
                actorUserId,
                correlationId,
                targetType,
                targetId,
                eventName,
                actionReason,
                oldSnapshot,
                newSnapshot,
                Instant.now()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getActionReason() {
        return actionReason;
    }

    public String getOldSnapshot() {
        return oldSnapshot;
    }

    public String getNewSnapshot() {
        return newSnapshot;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditEvent that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
