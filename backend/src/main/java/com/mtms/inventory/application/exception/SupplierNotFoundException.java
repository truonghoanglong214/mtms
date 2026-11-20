package com.mtms.inventory.application.exception;

import java.util.UUID;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(UUID supplierId) {
        super("Supplier not found with ID: " + supplierId);
    }

    public SupplierNotFoundException(String message) {
        super(message);
    }
}
