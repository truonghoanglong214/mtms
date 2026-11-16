package com.mtms.common.idempotency;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Thrown when an identical request is already in-progress (HTTP 409 Conflict).
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
