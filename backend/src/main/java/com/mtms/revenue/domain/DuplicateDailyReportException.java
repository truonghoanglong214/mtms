package com.mtms.revenue.domain;

public class DuplicateDailyReportException extends RuntimeException {
    public DuplicateDailyReportException(String message) {
        super(message);
    }
}
