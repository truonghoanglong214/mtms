package com.mtms.inventory.application.exception;

public class DuplicateUnitCodeException extends RuntimeException {

    public DuplicateUnitCodeException(String unitCode) {
        super("Unit code already exists: " + unitCode);
    }
}
