package com.mtms.finance.domain;

/**
 * P0-FIN-07 / BR-FIN-04
 * Thrown when a tax obligation is not found by ID.
 */
public class TaxObligationNotFoundException extends RuntimeException {

    public TaxObligationNotFoundException(String message) {
        super(message);
    }
}
