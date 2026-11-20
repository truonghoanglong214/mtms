package com.mtms.revenue.domain;

public class ReportAlreadyConfirmedException extends RuntimeException {
    public ReportAlreadyConfirmedException(String message) {
        super(message);
    }
}
