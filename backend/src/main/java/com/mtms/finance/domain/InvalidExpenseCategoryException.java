package com.mtms.finance.domain;

/**
 * Thrown when an expense is categorized under a non-OPERATING_EXPENSE group.
 */
public class InvalidExpenseCategoryException extends RuntimeException {

    public InvalidExpenseCategoryException(String message) {
        super(message);
    }
}
