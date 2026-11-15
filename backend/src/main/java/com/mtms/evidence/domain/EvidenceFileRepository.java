package com.mtms.evidence.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01
 * Domain repository interface for evidence file persistence.
 */
public interface EvidenceFileRepository {

    EvidenceFile save(EvidenceFile evidenceFile);

    Optional<EvidenceFile> findById(UUID id);

    List<EvidenceFile> findByBranchId(UUID branchId);

    boolean existsById(UUID id);
}
