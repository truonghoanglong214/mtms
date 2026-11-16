package com.mtms.importer.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * Domain model representing an Excel / CSV import batch.
 */
public class ImportBatch {

    private UUID id;
    private UUID branchId;
    private ImportKind importKind;
    private String sourceFilename;
    private String sourceSha256;
    private ImportBatchStatus status;
    private int totalRows;
    private int validRows;
    private int errorRows;
    private String previewSummary;
    private UUID committedBy;
    private Instant committedAt;
    private Instant createdAt;

    public ImportBatch() {
    }

    public ImportBatch(
            UUID id,
            UUID branchId,
            ImportKind importKind,
            String sourceFilename,
            String sourceSha256,
            ImportBatchStatus status,
            int totalRows,
            int validRows,
            int errorRows,
            String previewSummary,
            UUID committedBy,
            Instant committedAt,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.importKind = Objects.requireNonNull(importKind, "importKind must not be null");
        this.sourceFilename = Objects.requireNonNull(sourceFilename, "sourceFilename must not be null");
        this.sourceSha256 = Objects.requireNonNull(sourceSha256, "sourceSha256 must not be null");
        this.status = status != null ? status : ImportBatchStatus.PREVIEW;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.errorRows = errorRows;
        this.previewSummary = previewSummary != null ? previewSummary : "{}";
        this.committedBy = committedBy;
        this.committedAt = committedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ImportBatch create(
            UUID branchId,
            ImportKind importKind,
            String sourceFilename,
            String sourceSha256,
            int totalRows,
            int validRows,
            int errorRows,
            String previewSummary
    ) {
        ImportBatchStatus initialStatus = (errorRows == 0 && totalRows > 0)
                ? ImportBatchStatus.VALIDATED
                : ImportBatchStatus.PREVIEW;
        return new ImportBatch(
                UUID.randomUUID(),
                branchId,
                importKind,
                sourceFilename,
                sourceSha256,
                initialStatus,
                totalRows,
                validRows,
                errorRows,
                previewSummary,
                null,
                null,
                Instant.now()
        );
    }

    public void commit(UUID committedBy) {
        if (this.status != ImportBatchStatus.VALIDATED) {
            throw new IllegalArgumentException("Cannot commit batch: current status is " + this.status + " (must be VALIDATED)");
        }
        if (this.errorRows > 0) {
            throw new IllegalArgumentException("Cannot commit batch: batch contains " + this.errorRows + " error rows");
        }
        this.status = ImportBatchStatus.COMMITTED;
        this.committedBy = committedBy;
        this.committedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public ImportKind getImportKind() {
        return importKind;
    }

    public void setImportKind(ImportKind importKind) {
        this.importKind = importKind;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    public String getSourceSha256() {
        return sourceSha256;
    }

    public void setSourceSha256(String sourceSha256) {
        this.sourceSha256 = sourceSha256;
    }

    public ImportBatchStatus getStatus() {
        return status;
    }

    public void setStatus(ImportBatchStatus status) {
        this.status = status;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getErrorRows() {
        return errorRows;
    }

    public void setErrorRows(int errorRows) {
        this.errorRows = errorRows;
    }

    public String getPreviewSummary() {
        return previewSummary;
    }

    public void setPreviewSummary(String previewSummary) {
        this.previewSummary = previewSummary;
    }

    public UUID getCommittedBy() {
        return committedBy;
    }

    public void setCommittedBy(UUID committedBy) {
        this.committedBy = committedBy;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
