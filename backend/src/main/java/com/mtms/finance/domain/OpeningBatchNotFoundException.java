package com.mtms.finance.domain;

/**
 * P0-FIN-08 / BR-FIN-08
 * Thrown when an opening balance batch is not found.
 */
public class OpeningBatchNotFoundException extends RuntimeException {
    public OpeningBatchNotFoundException(String message) {
        super(message);
    }
}
