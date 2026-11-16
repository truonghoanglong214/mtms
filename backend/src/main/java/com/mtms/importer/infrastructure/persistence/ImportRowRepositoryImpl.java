package com.mtms.importer.infrastructure.persistence;

import com.mtms.importer.domain.ImportRow;
import com.mtms.importer.domain.ImportRowRepository;
import com.mtms.importer.domain.ImportRowStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * Persistence adapter bridging domain ImportRowRepository to Spring Data JPA.
 */
@Repository
public class ImportRowRepositoryImpl implements ImportRowRepository {

    private final ImportRowJpaRepository jpaRepository;

    public ImportRowRepositoryImpl(ImportRowJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ImportRow save(ImportRow row) {
        ImportRowJpaEntity entity = ImportRowJpaEntity.fromDomain(row);
        ImportRowJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<ImportRow> saveAll(List<ImportRow> rows) {
        List<ImportRowJpaEntity> entities = rows.stream()
                .map(ImportRowJpaEntity::fromDomain)
                .toList();
        return jpaRepository.saveAll(entities)
                .stream()
                .map(ImportRowJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<ImportRow> findById(UUID id) {
        return jpaRepository.findById(id).map(ImportRowJpaEntity::toDomain);
    }

    @Override
    public List<ImportRow> findByBatchId(UUID importBatchId) {
        return jpaRepository.findByImportBatchIdOrderByRowNumberAsc(importBatchId)
                .stream()
                .map(ImportRowJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<ImportRow> findByBatchIdAndStatus(UUID importBatchId, ImportRowStatus status) {
        return jpaRepository.findByImportBatchIdAndStatusOrderByRowNumberAsc(importBatchId, status)
                .stream()
                .map(ImportRowJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByBatchId(UUID importBatchId) {
        jpaRepository.deleteByImportBatchId(importBatchId);
    }
}
