package com.mtms.revenue.domain;

public class MissingCloseReasonException extends RuntimeException {
    public MissingCloseReasonException(String message) {
        super(message);
    }
}
