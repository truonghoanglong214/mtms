package com.mtms.evidence.infrastructure.persistence;

import com.mtms.evidence.domain.VisibilityClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01
 * JPA persistence entity mapped to mtms.evidence_files.
 */
@Entity
@Table(name = "evidence_files", schema = "mtms")
public class EvidenceFileJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "storage_object_key", nullable = false, unique = true)
    private String storageObjectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_class", nullable = false, length = 32)
    private VisibilityClass visibilityClass;

    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public EvidenceFileJpaEntity() {
    }

    public EvidenceFileJpaEntity(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String storageObjectKey,
            String originalFilename,
            String contentType,
            long byteSize,
            String checksumSha256,
            VisibilityClass visibilityClass,
            LocalDate retentionUntil,
            UUID uploadedBy,
            Instant createdAt
    ) {
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.storageObjectKey = storageObjectKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.checksumSha256 = checksumSha256;
        this.visibilityClass = visibilityClass;
        this.retentionUntil = retentionUntil;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public void setBusinessEntityId(UUID businessEntityId) {
        this.businessEntityId = businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public String getStorageObjectKey() {
        return storageObjectKey;
    }

    public void setStorageObjectKey(String storageObjectKey) {
        this.storageObjectKey = storageObjectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getByteSize() {
        return byteSize;
    }

    public void setByteSize(long byteSize) {
        this.byteSize = byteSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public VisibilityClass getVisibilityClass() {
        return visibilityClass;
    }

    public void setVisibilityClass(VisibilityClass visibilityClass) {
        this.visibilityClass = visibilityClass;
    }

    public LocalDate getRetentionUntil() {
        return retentionUntil;
    }

    public void setRetentionUntil(LocalDate retentionUntil) {
        this.retentionUntil = retentionUntil;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
