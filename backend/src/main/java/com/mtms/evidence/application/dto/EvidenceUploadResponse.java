package com.mtms.evidence.application.dto;

import com.mtms.evidence.domain.VisibilityClass;

import java.time.Instant;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01
 * Response payload returned upon successful evidence file upload.
 */
public record EvidenceUploadResponse(
        UUID id,
        UUID branchId,
        String originalFilename,
        String contentType,
        long byteSize,
        String checksumSha256,
        VisibilityClass visibilityClass,
        UUID uploadedBy,
        Instant createdAt
) {
}
