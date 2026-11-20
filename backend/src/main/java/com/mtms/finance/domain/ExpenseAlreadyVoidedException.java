package com.mtms.finance.domain;

/**
 * Thrown when trying to confirm or void an expense that is already VOIDED.
 */
public class ExpenseAlreadyVoidedException extends RuntimeException {

    public ExpenseAlreadyVoidedException(String message) {
        super(message);
    }
}
