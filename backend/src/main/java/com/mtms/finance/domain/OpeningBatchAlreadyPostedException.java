package com.mtms.finance.domain;

/**
 * P0-FIN-08 / BR-FIN-08 / ADR-009
 * Thrown when attempting to edit or re-post an already posted opening batch.
 */
public class OpeningBatchAlreadyPostedException extends RuntimeException {
    public OpeningBatchAlreadyPostedException(String message) {
        super(message);
    }
}
