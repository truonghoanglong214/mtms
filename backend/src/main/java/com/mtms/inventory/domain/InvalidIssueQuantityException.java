package com.mtms.inventory.domain;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-03
 * Thrown when issue quantity is non-positive or invalid.
 */
public class InvalidIssueQuantityException extends RuntimeException {

    public InvalidIssueQuantityException(String message) {
        super(message);
    }
}
