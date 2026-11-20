package com.mtms.hr.domain;

public class PayrollPeriodNotFoundException extends RuntimeException {
    public PayrollPeriodNotFoundException(String message) {
        super(message);
    }
}
