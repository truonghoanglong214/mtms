package com.mtms.inventory.application.exception;

public class DuplicateItemCodeException extends RuntimeException {

    public DuplicateItemCodeException(String itemCode) {
        super("Catalog item code already exists in this entity: " + itemCode);
    }
}
