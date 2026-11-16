package com.mtms.importer.application.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.importer.domain.ImportBatch;
import com.mtms.importer.domain.ImportBatchStatus;
import com.mtms.importer.domain.ImportKind;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * DTO representing an import batch.
 */
public record ImportBatchDto(
        UUID id,
        UUID branchId,
        ImportKind importKind,
        String sourceFilename,
        String sourceSha256,
        ImportBatchStatus status,
        int totalRows,
        int validRows,
        int errorRows,
        Map<String, Object> previewSummary,
        UUID committedBy,
        Instant committedAt,
        Instant createdAt
) {
    public static ImportBatchDto fromDomain(ImportBatch batch, ObjectMapper objectMapper) {
        Map<String, Object> summaryMap = Collections.emptyMap();
        if (batch.getPreviewSummary() != null && !batch.getPreviewSummary().isBlank()) {
            try {
                summaryMap = objectMapper.readValue(batch.getPreviewSummary(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                summaryMap = Map.of("raw", batch.getPreviewSummary());
            }
        }
        return new ImportBatchDto(
                batch.getId(),
                batch.getBranchId(),
                batch.getImportKind(),
                batch.getSourceFilename(),
                batch.getSourceSha256(),
                batch.getStatus(),
                batch.getTotalRows(),
                batch.getValidRows(),
                batch.getErrorRows(),
                summaryMap,
                batch.getCommittedBy(),
                batch.getCommittedAt(),
                batch.getCreatedAt()
        );
    }
}
