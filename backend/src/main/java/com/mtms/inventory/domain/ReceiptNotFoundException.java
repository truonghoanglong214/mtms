package com.mtms.inventory.domain;

import java.util.UUID;

public class ReceiptNotFoundException extends RuntimeException {

    public ReceiptNotFoundException(UUID receiptId) {
        super("Purchase receipt not found: " + receiptId);
    }

    public ReceiptNotFoundException(String message) {
        super(message);
    }
}
