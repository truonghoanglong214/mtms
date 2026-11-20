package com.mtms.inventory.domain;

/**
 * P0-INV-03 / BR-INV-05 / TC-008
 * Status of branch payable debt record.
 */
public enum PayableStatus {
    OPEN,
    SETTLED,
    DISPUTED,
    CANCELLED
}
