package com.mtms.importer.infrastructure.persistence;

import com.mtms.importer.domain.ImportKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-04 / BR-SEC-02
 * Spring Data JPA repository for import batches.
 */
@Repository
public interface ImportBatchJpaRepository extends JpaRepository<ImportBatchJpaEntity, UUID> {

    Optional<ImportBatchJpaEntity> findByImportKindAndBranchIdAndSourceSha256(
            ImportKind importKind,
            UUID branchId,
            String sourceSha256
    );

    Optional<ImportBatchJpaEntity> findByBranchIdAndId(UUID branchId, UUID id);

    List<ImportBatchJpaEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);
}
