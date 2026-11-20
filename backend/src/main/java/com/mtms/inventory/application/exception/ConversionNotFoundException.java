package com.mtms.inventory.application.exception;

import java.time.LocalDate;
import java.util.UUID;

public class ConversionNotFoundException extends RuntimeException {

    public ConversionNotFoundException(UUID itemId, UUID supplierId, LocalDate date) {
        super(String.format("No active supplier pack conversion found for item %s, supplier %s on %s",
                itemId, supplierId != null ? supplierId : "ANY", date));
    }

    public ConversionNotFoundException(String message) {
        super(message);
    }
}
