package com.mtms.importer.infrastructure.persistence;

import com.mtms.importer.domain.ImportRowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * Spring Data JPA repository for import rows.
 */
@Repository
public interface ImportRowJpaRepository extends JpaRepository<ImportRowJpaEntity, UUID> {

    List<ImportRowJpaEntity> findByImportBatchIdOrderByRowNumberAsc(UUID importBatchId);

    List<ImportRowJpaEntity> findByImportBatchIdAndStatusOrderByRowNumberAsc(
            UUID importBatchId,
            ImportRowStatus status
    );

    void deleteByImportBatchId(UUID importBatchId);
}
