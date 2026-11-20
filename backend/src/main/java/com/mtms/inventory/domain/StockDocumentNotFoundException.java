package com.mtms.inventory.domain;

import java.util.UUID;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-03
 * Thrown when a stock document is not found.
 */
public class StockDocumentNotFoundException extends RuntimeException {

    public StockDocumentNotFoundException(UUID documentId) {
        super("Stock document not found with ID: " + documentId);
    }

    public StockDocumentNotFoundException(String message) {
        super(message);
    }
}
