package com.mtms.evidence.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01
 * Spring Data JPA repository for evidence file entities.
 */
@Repository
public interface EvidenceFileJpaRepository extends JpaRepository<EvidenceFileJpaEntity, UUID> {

    List<EvidenceFileJpaEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    Optional<EvidenceFileJpaEntity> findByStorageObjectKey(String storageObjectKey);
}
