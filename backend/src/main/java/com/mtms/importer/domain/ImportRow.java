package com.mtms.importer.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * Domain model representing an individual imported row.
 */
public class ImportRow {

    private UUID id;
    private UUID importBatchId;
    private int rowNumber;
    private String rawData;
    private String normalizedData;
    private String errors;
    private ImportRowStatus status;

    public ImportRow() {
    }

    public ImportRow(
            UUID id,
            UUID importBatchId,
            int rowNumber,
            String rawData,
            String normalizedData,
            String errors,
            ImportRowStatus status
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.importBatchId = Objects.requireNonNull(importBatchId, "importBatchId must not be null");
        if (rowNumber <= 0) {
            throw new IllegalArgumentException("rowNumber must be positive (> 0)");
        }
        this.rowNumber = rowNumber;
        this.rawData = rawData != null ? rawData : "{}";
        this.normalizedData = normalizedData;
        this.errors = errors != null ? errors : "[]";
        this.status = status != null ? status : ImportRowStatus.PENDING;
    }

    public static ImportRow create(
            UUID importBatchId,
            int rowNumber,
            String rawData,
            String normalizedData,
            String errors,
            ImportRowStatus status
    ) {
        return new ImportRow(
                UUID.randomUUID(),
                importBatchId,
                rowNumber,
                rawData,
                normalizedData,
                errors,
                status
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public void setImportBatchId(UUID importBatchId) {
        this.importBatchId = importBatchId;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public String getNormalizedData() {
        return normalizedData;
    }

    public void setNormalizedData(String normalizedData) {
        this.normalizedData = normalizedData;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public ImportRowStatus getStatus() {
        return status;
    }

    public void setStatus(ImportRowStatus status) {
        this.status = status;
    }
}
