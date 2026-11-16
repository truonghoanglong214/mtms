package com.mtms.importer.application.validation;

import com.mtms.importer.domain.ImportRowStatus;
import com.mtms.importer.domain.RowValidationError;

import java.util.List;
import java.util.Map;

/**
 * P0-SEC-04 / BR-SEC-02 / TC-045
 * Representation of a validated row with errors and computed row status.
 */
public record ValidatedRow(
        int rowNumber,
        Map<String, String> rawData,
        Map<String, Object> normalizedData,
        List<RowValidationError> errors,
        ImportRowStatus status
) {
}
