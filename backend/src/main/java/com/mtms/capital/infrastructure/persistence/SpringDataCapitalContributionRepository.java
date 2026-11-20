package com.mtms.capital.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataCapitalContributionRepository extends JpaRepository<CapitalContributionJpaEntity, UUID> {
    List<CapitalContributionJpaEntity> findByCommitmentId(UUID commitmentId);

    @Query("SELECT cb FROM CapitalContributionJpaEntity cb WHERE cb.commitmentId IN " +
           "(SELECT c.id FROM CapitalCommitmentJpaEntity c WHERE c.agreementId IN " +
           "(SELECT a.id FROM InvestorAgreementJpaEntity a WHERE a.branchId = :branchId))")
    List<CapitalContributionJpaEntity> findByBranchId(@Param("branchId") UUID branchId);
}
