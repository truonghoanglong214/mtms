package com.mtms.finance.domain;

/**
 * P0-FIN-05 / BR-FIN-02
 * Thrown when trying to settle a payable that is already settled.
 */
public class PayableAlreadySettledException extends RuntimeException {

    public PayableAlreadySettledException(String message) {
        super(message);
    }
}
