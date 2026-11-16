package com.mtms.common.idempotency;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Status indicating whether a key acquisition resulted in a new lock or a cached replay.
 */
public enum AcquireStatus {
    ACQUIRED,
    CACHED
}
