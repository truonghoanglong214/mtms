package com.mtms.finance.domain;

/**
 * P0-FIN-07 / BR-FIN-04
 * Thrown when tax payment amount exceeds the remaining liability (amountDue - amountPaid).
 */
public class TaxPaymentExceedsRemainingLiabilityException extends RuntimeException {

    public TaxPaymentExceedsRemainingLiabilityException(String message) {
        super(message);
    }
}
