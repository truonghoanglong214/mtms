package com.mtms.revenue.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-REV-01 / BR-REV-02 / TC-005
 * Domain entity representing branch business calendar status for a specific date.
 * If operationStatus is CLOSED, closeReason is strictly required.
 */
public class BusinessDay {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final LocalDate businessDate;
    private OperationStatus operationStatus;
    private String closeReason;
    private UUID recordedBy;
    private final Instant createdAt;
    private Instant updatedAt;

    public BusinessDay(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            LocalDate businessDate,
            OperationStatus operationStatus,
            String closeReason,
            UUID recordedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.businessDate = Objects.requireNonNull(businessDate, "businessDate must not be null");
        this.recordedBy = Objects.requireNonNull(recordedBy, "recordedBy must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        setStatusAndReason(operationStatus, closeReason);
    }

    public static BusinessDay create(
            UUID businessEntityId,
            UUID branchId,
            LocalDate businessDate,
            OperationStatus operationStatus,
            String closeReason,
            UUID recordedBy
    ) {
        Instant now = Instant.now();
        return new BusinessDay(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                businessDate,
                operationStatus,
                closeReason,
                recordedBy,
                now,
                now
        );
    }

    public void updateStatus(OperationStatus newStatus, String newReason, UUID actorUserId) {
        setStatusAndReason(newStatus, newReason);
        this.recordedBy = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        this.updatedAt = Instant.now();
    }

    private void setStatusAndReason(OperationStatus status, String reason) {
        OperationStatus effectiveStatus = status != null ? status : OperationStatus.OPEN;
        if (effectiveStatus == OperationStatus.CLOSED) {
            if (reason == null || reason.trim().isEmpty()) {
                throw new MissingCloseReasonException("Close reason is mandatory when operation status is CLOSED");
            }
            this.closeReason = reason.trim();
        } else {
            this.closeReason = null;
        }
        this.operationStatus = effectiveStatus;
    }

    public boolean isClosed() {
        return this.operationStatus == OperationStatus.CLOSED;
    }

    public boolean isOpen() {
        return this.operationStatus == OperationStatus.OPEN;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
