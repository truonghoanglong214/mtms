package com.mtms.importer.domain;

/**
 * P0-SEC-04 / BR-SEC-02
 * Status lifecycle of an import batch.
 */
public enum ImportBatchStatus {
    PREVIEW,
    VALIDATED,
    COMMITTED,
    REJECTED
}
