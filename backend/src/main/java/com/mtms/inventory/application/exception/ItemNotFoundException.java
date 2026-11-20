package com.mtms.inventory.application.exception;

import java.util.UUID;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(UUID itemId) {
        super("Catalog item not found with ID: " + itemId);
    }

    public ItemNotFoundException(String message) {
        super(message);
    }
}
