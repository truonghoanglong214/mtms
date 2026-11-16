package com.mtms.common.idempotency;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Result of trying to acquire an idempotency key.
 */
public record AcquireResult(
        AcquireStatus status,
        IdempotencyRecord record
) {
    public static AcquireResult acquired(IdempotencyRecord record) {
        return new AcquireResult(AcquireStatus.ACQUIRED, record);
    }

    public static AcquireResult cached(IdempotencyRecord record) {
        return new AcquireResult(AcquireStatus.CACHED, record);
    }

    public boolean isAcquired() {
        return status == AcquireStatus.ACQUIRED;
    }

    public boolean isCached() {
        return status == AcquireStatus.CACHED;
    }
}
