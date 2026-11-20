package com.mtms.hr.domain;

/**
 * P0-HR-07
 * Thrown when a requested payroll payment record is not found.
 */
public class PayrollPaymentNotFoundException extends RuntimeException {
    public PayrollPaymentNotFoundException(String message) {
        super(message);
    }
}
