package com.mtms.inventory.application.exception;

import java.util.UUID;

public class ImportBatchNotFoundException extends RuntimeException {
    public ImportBatchNotFoundException(UUID batchId) {
        super("Import batch with ID " + batchId + " not found");
    }
}
