package com.mtms.evidence.application.dto;

import com.mtms.evidence.domain.EvidenceFile;
import com.mtms.evidence.domain.VisibilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01
 * Read DTO containing full evidence file metadata.
 */
public record EvidenceFileDto(
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
    public static EvidenceFileDto from(EvidenceFile file) {
        return new EvidenceFileDto(
                file.getId(),
                file.getBusinessEntityId(),
                file.getBranchId(),
                file.getStorageObjectKey(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getByteSize(),
                file.getChecksumSha256(),
                file.getVisibilityClass(),
                file.getRetentionUntil(),
                file.getUploadedBy(),
                file.getCreatedAt()
        );
    }
}
