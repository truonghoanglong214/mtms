package com.mtms.hr.domain;

public class PayrollLineNotFoundException extends RuntimeException {
    public PayrollLineNotFoundException(String message) {
        super(message);
    }
}
