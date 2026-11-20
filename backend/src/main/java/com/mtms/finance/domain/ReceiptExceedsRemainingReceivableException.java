package com.mtms.finance.domain;

/**
 * P0-FIN-05
 * Thrown when receipt amount exceeds remaining receivable balance.
 */
public class ReceiptExceedsRemainingReceivableException extends RuntimeException {

    public ReceiptExceedsRemainingReceivableException(String message) {
        super(message);
    }
}
