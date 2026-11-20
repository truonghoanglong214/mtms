package com.mtms.finance.domain;

/**
 * P0-FIN-07 / BR-FIN-04
 * Thrown when attempting to cancel a tax obligation that has recorded payments.
 */
public class CannotCancelPaidTaxException extends RuntimeException {

    public CannotCancelPaidTaxException(String message) {
        super(message);
    }
}
