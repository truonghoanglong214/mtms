package com.mtms.hr.domain;

public class PayrollAlreadyApprovedException extends RuntimeException {
    public PayrollAlreadyApprovedException(String message) {
        super(message);
    }
}
