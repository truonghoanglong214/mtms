package com.mtms.importer.infrastructure.persistence;

import com.mtms.importer.domain.ImportBatch;
import com.mtms.importer.domain.ImportBatchStatus;
import com.mtms.importer.domain.ImportKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * JPA entity mapping mtms.import_batches.
 */
@Entity
@Table(name = "import_batches", schema = "mtms")
public class ImportBatchJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_kind", nullable = false, length = 48)
    private ImportKind importKind;

    @Column(name = "source_filename", nullable = false, length = 255)
    private String sourceFilename;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_sha256", length = 64, nullable = false)
    private String sourceSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ImportBatchStatus status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "error_rows", nullable = false)
    private int errorRows;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preview_summary", nullable = false)
    private String previewSummary;

    @Column(name = "committed_by")
    private UUID committedBy;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ImportBatchJpaEntity() {
    }

    public ImportBatchJpaEntity(
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
        this.id = id;
        this.branchId = branchId;
        this.importKind = importKind;
        this.sourceFilename = sourceFilename;
        this.sourceSha256 = sourceSha256;
        this.status = status;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.errorRows = errorRows;
        this.previewSummary = previewSummary != null ? previewSummary : "{}";
        this.committedBy = committedBy;
        this.committedAt = committedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ImportBatchJpaEntity fromDomain(ImportBatch domain) {
        Objects.requireNonNull(domain, "Domain ImportBatch must not be null");
        return new ImportBatchJpaEntity(
                domain.getId(),
                domain.getBranchId(),
                domain.getImportKind(),
                domain.getSourceFilename(),
                domain.getSourceSha256(),
                domain.getStatus(),
                domain.getTotalRows(),
                domain.getValidRows(),
                domain.getErrorRows(),
                domain.getPreviewSummary(),
                domain.getCommittedBy(),
                domain.getCommittedAt(),
                domain.getCreatedAt()
        );
    }

    public ImportBatch toDomain() {
        return new ImportBatch(
                this.id,
                this.branchId,
                this.importKind,
                this.sourceFilename,
                this.sourceSha256,
                this.status,
                this.totalRows,
                this.validRows,
                this.errorRows,
                this.previewSummary,
                this.committedBy,
                this.committedAt,
                this.createdAt
        );
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
