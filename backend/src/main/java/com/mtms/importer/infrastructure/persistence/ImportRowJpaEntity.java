package com.mtms.importer.infrastructure.persistence;

import com.mtms.importer.domain.ImportRow;
import com.mtms.importer.domain.ImportRowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * JPA entity mapping mtms.import_rows.
 */
@Entity
@Table(name = "import_rows", schema = "mtms")
public class ImportRowJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "import_batch_id", nullable = false)
    private UUID importBatchId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false)
    private String rawData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_data")
    private String normalizedData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "errors", nullable = false)
    private String errors;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ImportRowStatus status;

    public ImportRowJpaEntity() {
    }

    public ImportRowJpaEntity(
            UUID id,
            UUID importBatchId,
            int rowNumber,
            String rawData,
            String normalizedData,
            String errors,
            ImportRowStatus status
    ) {
        this.id = id;
        this.importBatchId = importBatchId;
        this.rowNumber = rowNumber;
        this.rawData = rawData != null ? rawData : "{}";
        this.normalizedData = normalizedData;
        this.errors = errors != null ? errors : "[]";
        this.status = status;
    }

    public static ImportRowJpaEntity fromDomain(ImportRow domain) {
        Objects.requireNonNull(domain, "Domain ImportRow must not be null");
        return new ImportRowJpaEntity(
                domain.getId(),
                domain.getImportBatchId(),
                domain.getRowNumber(),
                domain.getRawData(),
                domain.getNormalizedData(),
                domain.getErrors(),
                domain.getStatus()
        );
    }

    public ImportRow toDomain() {
        return new ImportRow(
                this.id,
                this.importBatchId,
                this.rowNumber,
                this.rawData,
                this.normalizedData,
                this.errors,
                this.status
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
