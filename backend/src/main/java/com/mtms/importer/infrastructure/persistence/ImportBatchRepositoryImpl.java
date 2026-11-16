package com.mtms.importer.infrastructure.persistence;

import com.mtms.importer.domain.ImportBatch;
import com.mtms.importer.domain.ImportBatchRepository;
import com.mtms.importer.domain.ImportKind;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * Persistence adapter bridging domain ImportBatchRepository to Spring Data JPA.
 */
@Repository
public class ImportBatchRepositoryImpl implements ImportBatchRepository {

    private final ImportBatchJpaRepository jpaRepository;

    public ImportBatchRepositoryImpl(ImportBatchJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ImportBatch save(ImportBatch batch) {
        ImportBatchJpaEntity entity = ImportBatchJpaEntity.fromDomain(batch);
        ImportBatchJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<ImportBatch> findById(UUID id) {
        return jpaRepository.findById(id).map(ImportBatchJpaEntity::toDomain);
    }

    @Override
    public Optional<ImportBatch> findByBranchIdAndId(UUID branchId, UUID id) {
        return jpaRepository.findByBranchIdAndId(branchId, id).map(ImportBatchJpaEntity::toDomain);
    }

    @Override
    public Optional<ImportBatch> findByKindBranchAndSha256(ImportKind importKind, UUID branchId, String sourceSha256) {
        return jpaRepository.findByImportKindAndBranchIdAndSourceSha256(importKind, branchId, sourceSha256)
                .map(ImportBatchJpaEntity::toDomain);
    }

    @Override
    public List<ImportBatch> findByBranchId(UUID branchId) {
        return jpaRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
                .stream()
                .map(ImportBatchJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void delete(ImportBatch batch) {
        jpaRepository.deleteById(batch.getId());
    }
}
