package com.mtms.importer.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * Domain repository contract for import rows.
 */
public interface ImportRowRepository {

    ImportRow save(ImportRow row);

    List<ImportRow> saveAll(List<ImportRow> rows);

    Optional<ImportRow> findById(UUID id);

    List<ImportRow> findByBatchId(UUID importBatchId);

    List<ImportRow> findByBatchIdAndStatus(UUID importBatchId, ImportRowStatus status);

    void deleteByBatchId(UUID importBatchId);
}
