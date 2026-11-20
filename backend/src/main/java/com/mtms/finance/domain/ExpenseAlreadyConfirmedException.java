package com.mtms.finance.domain;

/**
 * Thrown when trying to confirm an expense that is already in CONFIRMED state.
 */
public class ExpenseAlreadyConfirmedException extends RuntimeException {

    public ExpenseAlreadyConfirmedException(String message) {
        super(message);
    }
}
