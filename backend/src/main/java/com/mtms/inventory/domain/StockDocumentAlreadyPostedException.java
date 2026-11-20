package com.mtms.inventory.domain;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-03
 * Thrown when attempting to modify or post an already finalized stock document.
 */
public class StockDocumentAlreadyPostedException extends RuntimeException {

    public StockDocumentAlreadyPostedException(String message) {
        super(message);
    }
}
