package com.mtms.inventory.application.exception;

/**
 * P0-INV-06 / P0-INV-07
 * Exception thrown when startDate is after endDate in reporting queries.
 */
public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
