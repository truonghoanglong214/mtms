package com.mtms.movement.domain.model;

/**
 * P1-MOV-01 / P1-MOV-02
 * Lifecycle states of an intra-entity stock transfer.
 */
public enum TransferStatus {
    DRAFT,
    DISPATCHED,
    PARTIAL_RECEIPT,
    COMPLETED,
    CANCELLED
}
