package com.mtms.inventory.domain;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-03
 * Lifecycle status of stock documents.
 */
public enum StockDocumentStatus {
    DRAFT,
    SUBMITTED,
    POSTED,
    CANCELLED
}
