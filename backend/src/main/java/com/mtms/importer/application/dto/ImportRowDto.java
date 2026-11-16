package com.mtms.importer.application.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.importer.domain.ImportRow;
import com.mtms.importer.domain.ImportRowStatus;
import com.mtms.importer.domain.RowValidationError;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * DTO representing an individual staged import row with validation errors.
 */
public record ImportRowDto(
        UUID id,
        UUID importBatchId,
        int rowNumber,
        Map<String, Object> rawData,
        Map<String, Object> normalizedData,
        List<RowValidationError> errors,
        ImportRowStatus status
) {
    public static ImportRowDto fromDomain(ImportRow row, ObjectMapper objectMapper) {
        Map<String, Object> raw = Collections.emptyMap();
        Map<String, Object> norm = Collections.emptyMap();
        List<RowValidationError> errs = Collections.emptyList();

        try {
            if (row.getRawData() != null && !row.getRawData().isBlank()) {
                raw = objectMapper.readValue(row.getRawData(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {
        }

        try {
            if (row.getNormalizedData() != null && !row.getNormalizedData().isBlank()) {
                norm = objectMapper.readValue(row.getNormalizedData(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {
        }

        try {
            if (row.getErrors() != null && !row.getErrors().isBlank()) {
                errs = objectMapper.readValue(row.getErrors(), new TypeReference<List<RowValidationError>>() {});
            }
        } catch (Exception ignored) {
        }

        return new ImportRowDto(
                row.getId(),
                row.getImportBatchId(),
                row.getRowNumber(),
                raw,
                norm,
                errs,
                row.getStatus()
        );
    }
}
