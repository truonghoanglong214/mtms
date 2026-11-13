package com.mtms.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchJpaRepository extends JpaRepository<BranchJpaEntity, UUID> {

    boolean existsByBusinessEntityIdAndBranchCode(UUID businessEntityId, String branchCode);

    List<BranchJpaEntity> findByBusinessEntityId(UUID businessEntityId);

    Optional<BranchJpaEntity> findByBusinessEntityIdAndId(UUID businessEntityId, UUID id);
}
