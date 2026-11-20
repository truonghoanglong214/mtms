package com.mtms.finance.domain;

/**
 * P0-FIN-07 / BR-FIN-04
 * Thrown when an obligation code already exists within a business entity.
 */
public class TaxObligationAlreadyExistsException extends RuntimeException {

    public TaxObligationAlreadyExistsException(String message) {
        super(message);
    }
}
