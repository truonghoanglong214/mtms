package com.mtms.inventory.domain;

public class ReceiptAlreadyPostedException extends RuntimeException {

    public ReceiptAlreadyPostedException(String message) {
        super(message);
    }
}
