package com.mtms.inventory.domain;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-02 / TC-021
 * Thrown when requested stock issue quantity exceeds available stock balance.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
