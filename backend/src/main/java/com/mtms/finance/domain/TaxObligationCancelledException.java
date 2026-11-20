package com.mtms.finance.domain;

/**
 * P0-FIN-07 / BR-FIN-04
 * Thrown when attempting to pay or modify a cancelled tax obligation.
 */
public class TaxObligationCancelledException extends RuntimeException {

    public TaxObligationCancelledException(String message) {
        super(message);
    }
}
