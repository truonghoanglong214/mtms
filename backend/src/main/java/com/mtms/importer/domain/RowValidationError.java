package com.mtms.importer.domain;

import java.io.Serializable;

/**
 * P0-SEC-04 / BR-SEC-02 / TC-045
 * Granular row-level validation error details.
 */
public record RowValidationError(
        String field,
        String error,
        String message
) implements Serializable {
}
