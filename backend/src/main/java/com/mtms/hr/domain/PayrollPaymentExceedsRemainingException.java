package com.mtms.hr.domain;

/**
 * P0-HR-07 / BR-HR-06
 * Thrown when attempting to pay an amount exceeding the remaining unpaid gross salary.
 */
public class PayrollPaymentExceedsRemainingException extends RuntimeException {
    public PayrollPaymentExceedsRemainingException(String message) {
        super(message);
    }
}
