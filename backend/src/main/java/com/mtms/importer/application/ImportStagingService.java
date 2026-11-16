package com.mtms.importer.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.audit.application.AuditLogService;
import com.mtms.audit.application.dto.RecordAuditEventCommand;
import com.mtms.common.error.ResourceConflictException;
import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.importer.application.dto.ImportBatchDto;
import com.mtms.importer.application.dto.ImportRowDto;
import com.mtms.importer.application.validation.ImportValidator;
import com.mtms.importer.application.validation.ValidatedRow;
import com.mtms.importer.domain.ImportBatch;
import com.mtms.importer.domain.ImportBatchRepository;
import com.mtms.importer.domain.ImportBatchStatus;
import com.mtms.importer.domain.ImportKind;
import com.mtms.importer.domain.ImportRow;
import com.mtms.importer.domain.ImportRowRepository;
import com.mtms.importer.domain.ImportRowStatus;
import com.mtms.importer.infrastructure.parser.FileImportParser;
import com.mtms.importer.infrastructure.parser.ParsedImportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02 / TC-045
 * Orchestrates file staging, formula sanitization, business validation, preview generation, and batch commit.
 */
@Service
@Transactional
public class ImportStagingService {

    private static final Logger log = LoggerFactory.getLogger(ImportStagingService.class);

    private final ImportBatchRepository batchRepository;
    private final ImportRowRepository rowRepository;
    private final FileImportParser fileImportParser;
    private final ImportValidator importValidator;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    public ImportStagingService(
            ImportBatchRepository batchRepository,
            ImportRowRepository rowRepository,
            FileImportParser fileImportParser,
            ImportValidator importValidator,
            ObjectMapper objectMapper,
            @Autowired(required = false) AuditLogService auditLogService
    ) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.fileImportParser = fileImportParser;
        this.importValidator = importValidator;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    /**
     * Parses an uploaded spreadsheet or CSV, validates all rows, stores staging batch & rows,
     * and produces preview statistics.
     */
    public ImportBatchDto stageFile(UUID branchId, ImportKind kind, MultipartFile file, UUID actorUserId) {
        Objects.requireNonNull(branchId, "branchId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file bytes: " + e.getMessage(), e);
        }

        String sourceSha256 = computeSha256(fileBytes);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "import.xlsx";

        // Check if identical file was previously uploaded for this branch & kind
        Optional<ImportBatch> existingOpt = batchRepository.findByKindBranchAndSha256(kind, branchId, sourceSha256);
        if (existingOpt.isPresent()) {
            ImportBatch existing = existingOpt.get();
            if (existing.getStatus() == ImportBatchStatus.COMMITTED) {
                throw new ResourceConflictException("File with SHA-256 " + sourceSha256 + " has already been committed for branch " + branchId);
            }
            // Replace existing uncommitted preview batch
            rowRepository.deleteByBatchId(existing.getId());
            batchRepository.delete(existing);
        }

        // Parse file contents with formula sanitization
        ParsedImportData parsedData = fileImportParser.parse(file);

        // Validate rows and detect duplicate identifiers (TC-045)
        List<ValidatedRow> validatedRows = importValidator.validate(kind, parsedData);

        int totalRows = validatedRows.size();
        int validRows = (int) validatedRows.stream().filter(r -> r.status() == ImportRowStatus.VALID).count();
        int errorRows = totalRows - validRows;

        // Build preview summary JSON
        Map<String, Object> summaryMap = new LinkedHashMap<>();
        summaryMap.put("totalRows", totalRows);
        summaryMap.put("validRows", validRows);
        summaryMap.put("errorRows", errorRows);
        summaryMap.put("headers", parsedData.normalizedHeaders());
        summaryMap.put("sampleRows", validatedRows.stream().limit(5).map(ValidatedRow::normalizedData).toList());

        String previewSummaryJson;
        try {
            previewSummaryJson = objectMapper.writeValueAsString(summaryMap);
        } catch (Exception e) {
            previewSummaryJson = "{}";
        }

        ImportBatch batch = ImportBatch.create(
                branchId,
                kind,
                originalFilename,
                sourceSha256,
                totalRows,
                validRows,
                errorRows,
                previewSummaryJson
        );
        ImportBatch savedBatch = batchRepository.save(batch);

        // Persist all rows
        List<ImportRow> rowsToSave = new ArrayList<>();
        for (ValidatedRow vr : validatedRows) {
            String rawJson = toJson(vr.rawData());
            String normJson = toJson(vr.normalizedData());
            String errJson = toJson(vr.errors());

            rowsToSave.add(ImportRow.create(
                    savedBatch.getId(),
                    vr.rowNumber(),
                    rawJson,
                    normJson,
                    errJson,
                    vr.status()
            ));
        }
        rowRepository.saveAll(rowsToSave);

        return ImportBatchDto.fromDomain(savedBatch, objectMapper);
    }

    /**
     * Retrieves an import batch by ID within a specific branch.
     */
    @Transactional(readOnly = true)
    public ImportBatchDto getBatch(UUID branchId, UUID batchId) {
        ImportBatch batch = batchRepository.findByBranchIdAndId(branchId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Import batch not found: " + batchId));
        return ImportBatchDto.fromDomain(batch, objectMapper);
    }

    /**
     * Retrieves rows of a batch with an optional row status filter.
     */
    @Transactional(readOnly = true)
    public List<ImportRowDto> getBatchRows(UUID branchId, UUID batchId, ImportRowStatus statusFilter) {
        // Enforce branch boundary check
        batchRepository.findByBranchIdAndId(branchId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Import batch not found: " + batchId));

        List<ImportRow> rows = (statusFilter != null)
                ? rowRepository.findByBatchIdAndStatus(batchId, statusFilter)
                : rowRepository.findByBatchId(batchId);

        return rows.stream()
                .map(r -> ImportRowDto.fromDomain(r, objectMapper))
                .toList();
    }

    /**
     * Commits a validated import batch. Only batches with status VALIDATED and 0 error rows can be committed.
     */
    public ImportBatchDto commitBatch(UUID branchId, UUID batchId, UUID actorUserId) {
        ImportBatch batch = batchRepository.findByBranchIdAndId(branchId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Import batch not found: " + batchId));

        if (batch.getStatus() == ImportBatchStatus.COMMITTED) {
            throw new ResourceConflictException("Import batch " + batchId + " has already been committed");
        }

        if (batch.getStatus() != ImportBatchStatus.VALIDATED) {
            throw new IllegalArgumentException(
                    "Cannot commit batch: current status is " + batch.getStatus() + " (only VALIDATED batches can be committed)"
            );
        }

        if (batch.getErrorRows() > 0) {
            throw new IllegalArgumentException(
                    "Cannot commit batch: batch contains " + batch.getErrorRows() + " errors"
            );
        }

        batch.commit(actorUserId);
        ImportBatch committed = batchRepository.save(batch);

        // Record audit event if audit service is available
        if (auditLogService != null) {
            try {
                auditLogService.recordEvent(RecordAuditEventCommand.of(
                        branchId,
                        actorUserId,
                        "IMPORT_BATCH",
                        batchId,
                        "IMPORT_BATCH_COMMITTED",
                        "Committed import batch " + batch.getSourceFilename() + " for kind " + batch.getImportKind(),
                        null,
                        objectMapper.writeValueAsString(Map.of(
                                "status", committed.getStatus(),
                                "totalRows", committed.getTotalRows(),
                                "validRows", committed.getValidRows(),
                                "committedAt", committed.getCommittedAt()
                        ))
                ));
            } catch (Exception e) {
                log.warn("Failed to record audit event for import batch commit: {}", e.getMessage());
            }
        }

        return ImportBatchDto.fromDomain(committed, objectMapper);
    }

    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
