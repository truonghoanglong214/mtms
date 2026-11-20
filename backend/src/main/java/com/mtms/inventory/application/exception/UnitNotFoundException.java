package com.mtms.inventory.application.exception;

import java.util.UUID;

public class UnitNotFoundException extends RuntimeException {

    public UnitNotFoundException(UUID unitId) {
        super("Measurement unit not found with ID: " + unitId);
    }

    public UnitNotFoundException(String message) {
        super(message);
    }
}
