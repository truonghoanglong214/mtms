package com.mtms.finance.domain;

/**
 * P0-FIN-08 / BR-FIN-08
 * Thrown when cutover date is missing or invalid.
 */
public class InvalidCutoverDateException extends RuntimeException {
    public InvalidCutoverDateException(String message) {
        super(message);
    }
}
