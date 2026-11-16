package com.mtms.audit.infrastructure.persistence;

import com.mtms.audit.domain.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-SEC-02 / BR-SEC-03 / TC-041
 * JPA entity mapping mtms.audit_events with JSONB snapshots.
 */
@Entity
@Table(name = "audit_events", schema = "mtms")
public class AuditEventJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "target_type", length = 80, nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "event_name", length = 90, nullable = false)
    private String eventName;

    @Column(name = "action_reason", columnDefinition = "text")
    private String actionReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_snapshot")
    private String oldSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_snapshot")
    private String newSnapshot;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public AuditEventJpaEntity() {
    }

    public AuditEventJpaEntity(
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
        this.id = id;
        this.branchId = branchId;
        this.actorUserId = actorUserId;
        this.correlationId = correlationId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.eventName = eventName;
        this.actionReason = actionReason;
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public static AuditEventJpaEntity fromDomain(AuditEvent domain) {
        Objects.requireNonNull(domain, "Domain AuditEvent must not be null");
        return new AuditEventJpaEntity(
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

    public AuditEvent toDomain() {
        return new AuditEvent(
                id,
                branchId,
                actorUserId,
                correlationId,
                targetType,
                targetId,
                eventName,
                actionReason,
                oldSnapshot,
                newSnapshot,
                occurredAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getActionReason() {
        return actionReason;
    }

    public void setActionReason(String actionReason) {
        this.actionReason = actionReason;
    }

    public String getOldSnapshot() {
        return oldSnapshot;
    }

    public void setOldSnapshot(String oldSnapshot) {
        this.oldSnapshot = oldSnapshot;
    }

    public String getNewSnapshot() {
        return newSnapshot;
    }

    public void setNewSnapshot(String newSnapshot) {
        this.newSnapshot = newSnapshot;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
