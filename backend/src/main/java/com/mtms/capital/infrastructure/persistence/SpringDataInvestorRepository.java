package com.mtms.capital.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataInvestorRepository extends JpaRepository<InvestorJpaEntity, UUID> {
    List<InvestorJpaEntity> findByEntityId(UUID entityId);
    Optional<InvestorJpaEntity> findByEntityIdAndDisplayName(UUID entityId, String displayName);
}
