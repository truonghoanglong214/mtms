package com.mtms.importer.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * Domain repository contract for import batches.
 */
public interface ImportBatchRepository {

    ImportBatch save(ImportBatch batch);

    Optional<ImportBatch> findById(UUID id);

    Optional<ImportBatch> findByBranchIdAndId(UUID branchId, UUID id);

    Optional<ImportBatch> findByKindBranchAndSha256(ImportKind importKind, UUID branchId, String sourceSha256);

    List<ImportBatch> findByBranchId(UUID branchId);

    void delete(ImportBatch batch);
}
