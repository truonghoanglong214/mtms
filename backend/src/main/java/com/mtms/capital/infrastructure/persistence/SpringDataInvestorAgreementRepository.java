package com.mtms.capital.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataInvestorAgreementRepository extends JpaRepository<InvestorAgreementJpaEntity, UUID> {
    List<InvestorAgreementJpaEntity> findByBranchId(UUID branchId);
    List<InvestorAgreementJpaEntity> findByInvestorId(UUID investorId);
    Optional<InvestorAgreementJpaEntity> findByBranchIdAndAgreementRef(UUID branchId, String agreementRef);
}
