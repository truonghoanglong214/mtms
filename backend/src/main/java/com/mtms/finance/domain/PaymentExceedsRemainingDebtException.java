package com.mtms.finance.domain;

/**
 * P0-FIN-05 / BR-FIN-02 / TC-009
 * Thrown when payment amount exceeds remaining debt on payable.
 */
public class PaymentExceedsRemainingDebtException extends RuntimeException {

    public PaymentExceedsRemainingDebtException(String message) {
        super(message);
    }
}
