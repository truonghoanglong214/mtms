package com.mtms.finance.domain;

/**
 * P0-FIN-07 / BR-FIN-04
 * Thrown when attempting to pay an already settled tax obligation.
 */
public class TaxObligationAlreadyPaidException extends RuntimeException {

    public TaxObligationAlreadyPaidException(String message) {
        super(message);
    }
}
