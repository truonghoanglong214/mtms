package com.mtms.inventory.application.exception;

public class DuplicateSupplierCodeException extends RuntimeException {

    public DuplicateSupplierCodeException(String supplierCode) {
        super("Supplier code already exists in this entity: " + supplierCode);
    }
}
