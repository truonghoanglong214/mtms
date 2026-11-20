package com.mtms.capital.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataProfitEntitlementTermRepository extends JpaRepository<ProfitEntitlementTermJpaEntity, UUID> {
    List<ProfitEntitlementTermJpaEntity> findByAgreementId(UUID agreementId);

    @Query("SELECT p FROM ProfitEntitlementTermJpaEntity p WHERE p.agreementId IN " +
           "(SELECT a.id FROM InvestorAgreementJpaEntity a WHERE a.branchId = :branchId)")
    List<ProfitEntitlementTermJpaEntity> findByBranchId(@Param("branchId") UUID branchId);
}
