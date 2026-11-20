package com.mtms.finance.domain;

/**
 * P0-FIN-01 / BR-FIN-01
 * Thrown when a user attempts to create a custom category under a disallowed group.
 * User-created categories must belong to OPERATING_EXPENSE.
 */
public class InvalidCategoryGroupException extends RuntimeException {
    public InvalidCategoryGroupException(String message) {
        super(message);
    }
}
