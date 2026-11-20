package com.mtms.inventory.domain;

/**
 * P0-INV-02 / BR-INV-08
 * Types of immutable stock ledger movements.
 */
public enum StockMovementType {
    OPENING_BALANCE,
    PURCHASE_RECEIPT,
    MANUAL_ISSUE,
    ADJUSTMENT,
    TRANSFER_OUT,
    TRANSFER_IN
}
