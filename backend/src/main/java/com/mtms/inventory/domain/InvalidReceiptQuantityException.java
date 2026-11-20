package com.mtms.inventory.domain;

public class InvalidReceiptQuantityException extends RuntimeException {

    public InvalidReceiptQuantityException(String message) {
        super(message);
    }
}
