package com.mtms.finance.domain;

/**
 * P0-FIN-08 / BR-FIN-08
 * Thrown when an opening batch already exists for the branch.
 */
public class OpeningBatchAlreadyExistsException extends RuntimeException {
    public OpeningBatchAlreadyExistsException(String message) {
        super(message);
    }
}
