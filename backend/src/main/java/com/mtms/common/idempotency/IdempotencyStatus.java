package com.mtms.common.idempotency;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Lifecycle status for request deduplication records.
 */
public enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
