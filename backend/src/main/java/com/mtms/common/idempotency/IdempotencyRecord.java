package com.mtms.common.idempotency;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Domain model representing an idempotency record.
 */
public class IdempotencyRecord {

    private final UUID id;
    private final UUID actorUserId;
    private final String idempotencyKey;
    private final String requestHash;
    private IdempotencyStatus status;
    private Integer responseStatus;
    private String responseHeaders;
    private String responseBody;
    private Instant expiresAt;
    private final Instant createdAt;

    public IdempotencyRecord(UUID id,
                             UUID actorUserId,
                             String idempotencyKey,
                             String requestHash,
                             IdempotencyStatus status,
                             Integer responseStatus,
                             String responseHeaders,
                             String responseBody,
                             Instant expiresAt,
                             Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.requestHash = Objects.requireNonNull(requestHash, "requestHash must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.responseStatus = responseStatus;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyStatus status) {
        this.status = status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isCompleted() {
        return status == IdempotencyStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return status == IdempotencyStatus.IN_PROGRESS;
    }

    public boolean isFailed() {
        return status == IdempotencyStatus.FAILED;
    }
}
