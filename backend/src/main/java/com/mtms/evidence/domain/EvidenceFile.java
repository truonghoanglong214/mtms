package com.mtms.evidence.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01 / TC-043
 * Immutable domain entity representing an uploaded evidence file with strict security boundaries.
 */
public class EvidenceFile {

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final String storageObjectKey;
    private final String originalFilename;
    private final String contentType;
    private final long byteSize;
    private final String checksumSha256;
    private final VisibilityClass visibilityClass;
    private final LocalDate retentionUntil;
    private final UUID uploadedBy;
    private final Instant createdAt;

    public EvidenceFile(
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
        this.id = Objects.requireNonNull(id, "Evidence file ID cannot be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "Business entity ID cannot be null");
        this.branchId = Objects.requireNonNull(branchId, "Branch ID cannot be null");
        this.storageObjectKey = Objects.requireNonNull(storageObjectKey, "Storage object key cannot be null");
        
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Original filename cannot be blank");
        }
        this.originalFilename = sanitizeFilename(originalFilename);

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType + 
                    ". Allowed types: " + ALLOWED_CONTENT_TYPES);
        }
        this.contentType = contentType.toLowerCase();

        if (byteSize <= 0) {
            throw new IllegalArgumentException("File size must be greater than 0 bytes");
        }
        if (byteSize > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 5MB (" + byteSize + " bytes)");
        }
        this.byteSize = byteSize;

        if (checksumSha256 == null || !checksumSha256.matches("^[a-fA-F0-9]{64}$")) {
            throw new IllegalArgumentException("Checksum must be a valid 64-character SHA-256 hexadecimal string");
        }
        this.checksumSha256 = checksumSha256.toLowerCase();

        this.visibilityClass = visibilityClass != null ? visibilityClass : VisibilityClass.BRANCH_PRIVATE;
        this.retentionUntil = retentionUntil;
        this.uploadedBy = Objects.requireNonNull(uploadedBy, "Uploader user ID cannot be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    private static String sanitizeFilename(String filename) {
        // Strip out path separators to avoid directory traversal
        String cleaned = filename.replace("\\", "/");
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash >= 0) {
            cleaned = cleaned.substring(lastSlash + 1);
        }
        return cleaned.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getStorageObjectKey() {
        return storageObjectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getByteSize() {
        return byteSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public VisibilityClass getVisibilityClass() {
        return visibilityClass;
    }

    public LocalDate getRetentionUntil() {
        return retentionUntil;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvidenceFile that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
