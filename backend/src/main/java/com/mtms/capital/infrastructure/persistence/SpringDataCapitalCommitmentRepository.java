package com.mtms.capital.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataCapitalCommitmentRepository extends JpaRepository<CapitalCommitmentJpaEntity, UUID> {
    List<CapitalCommitmentJpaEntity> findByAgreementId(UUID agreementId);

    @Query("SELECT c FROM CapitalCommitmentJpaEntity c WHERE c.agreementId IN " +
           "(SELECT a.id FROM InvestorAgreementJpaEntity a WHERE a.branchId = :branchId)")
    List<CapitalCommitmentJpaEntity> findByBranchId(@Param("branchId") UUID branchId);
}
