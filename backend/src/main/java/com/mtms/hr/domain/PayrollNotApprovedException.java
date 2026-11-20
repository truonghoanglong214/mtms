package com.mtms.hr.domain;

/**
 * P0-HR-07 / BR-HR-06
 * Thrown when attempting to pay salary on a payroll period that is not in APPROVED status.
 */
public class PayrollNotApprovedException extends RuntimeException {
    public PayrollNotApprovedException(String message) {
        super(message);
    }
}
