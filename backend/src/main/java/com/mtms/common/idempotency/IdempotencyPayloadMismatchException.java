package com.mtms.common.idempotency;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Thrown when an idempotency key is reused with a different request payload (HTTP 422 Unprocessable Entity).
 */
public class IdempotencyPayloadMismatchException extends RuntimeException {

    public IdempotencyPayloadMismatchException(String message) {
        super(message);
    }
}
