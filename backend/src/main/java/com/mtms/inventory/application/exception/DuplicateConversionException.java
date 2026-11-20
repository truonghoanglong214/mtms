package com.mtms.inventory.application.exception;

import java.time.LocalDate;

public class DuplicateConversionException extends RuntimeException {

    public DuplicateConversionException(String packName, LocalDate effectiveFrom) {
        super(String.format("Supplier pack conversion for pack '%s' with effectiveFrom '%s' already exists",
                packName, effectiveFrom));
    }
}
