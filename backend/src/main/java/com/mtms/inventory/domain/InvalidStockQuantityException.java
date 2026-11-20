package com.mtms.inventory.domain;

/**
 * P0-INV-02 / BR-INV-08
 * Thrown when stock quantity is <= 0 or invalid.
 */
public class InvalidStockQuantityException extends RuntimeException {
    public InvalidStockQuantityException(String message) {
        super(message);
    }
}
