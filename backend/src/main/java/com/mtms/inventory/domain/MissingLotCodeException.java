package com.mtms.inventory.domain;

/**
 * P0-INV-02 / BR-INV-08
 * Thrown when an item requiring lot tracking has no lot code specified.
 */
public class MissingLotCodeException extends RuntimeException {
    public MissingLotCodeException(String message) {
        super(message);
    }
}
