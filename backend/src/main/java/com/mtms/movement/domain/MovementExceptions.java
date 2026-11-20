package com.mtms.movement.domain;

import java.util.UUID;

/**
 * P1-MOV-01 / P1-MOV-02
 * Domain RFC 7807 Exception definitions for Movement operations.
 */
public final class MovementExceptions {

    private MovementExceptions() {}

    /**
     * TC-028: Inter-entity transfer is strictly forbidden.
     */
    public static class InterEntityTransferForbiddenException extends RuntimeException {
        public InterEntityTransferForbiddenException(String message) {
            super(message);
        }
    }

    public static class SameBranchTransferForbiddenException extends RuntimeException {
        public SameBranchTransferForbiddenException(String message) {
            super(message);
        }
    }

    public static class TransferNotFoundException extends RuntimeException {
        public TransferNotFoundException(UUID transferId) {
            super("Stock transfer not found: " + transferId);
        }
    }

    public static class InvalidTransferStatusException extends RuntimeException {
        public InvalidTransferStatusException(String message) {
            super(message);
        }
    }

    public static class TransferOverReceiptException extends RuntimeException {
        public TransferOverReceiptException(String message) {
            super(message);
        }
    }

    /**
     * TC-021: Negative or insufficient stock rejection.
     */
    public static class TransferInsufficientStockException extends RuntimeException {
        public TransferInsufficientStockException(String message) {
            super(message);
        }
    }
}
