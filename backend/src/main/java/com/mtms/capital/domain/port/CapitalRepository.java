package com.mtms.capital.domain.port;

import com.mtms.capital.domain.model.CapitalCommitment;
import com.mtms.capital.domain.model.CapitalContribution;
import com.mtms.capital.domain.model.Investor;
import com.mtms.capital.domain.model.InvestorAgreement;
import com.mtms.capital.domain.model.ProfitEntitlementTerm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapitalRepository {
    // Investor operations
    Investor saveInvestor(Investor investor);
    Optional<Investor> findInvestorById(UUID id);
    Optional<Investor> findInvestorByEntityAndDisplayName(UUID entityId, String displayName);
    List<Investor> findInvestorsByEntityId(UUID entityId);

    // Agreement operations
    InvestorAgreement saveAgreement(InvestorAgreement agreement);
    Optional<InvestorAgreement> findAgreementById(UUID id);
    Optional<InvestorAgreement> findAgreementByBranchAndRef(UUID branchId, String agreementRef);
    List<InvestorAgreement> findAgreementsByBranchId(UUID branchId);
    List<InvestorAgreement> findAgreementsByInvestorId(UUID investorId);

    // Commitment operations
    CapitalCommitment saveCommitment(CapitalCommitment commitment);
    Optional<CapitalCommitment> findCommitmentById(UUID id);
    List<CapitalCommitment> findCommitmentsByAgreementId(UUID agreementId);
    List<CapitalCommitment> findCommitmentsByBranchId(UUID branchId);

    // Contribution operations
    CapitalContribution saveContribution(CapitalContribution contribution);
    Optional<CapitalContribution> findContributionById(UUID id);
    List<CapitalContribution> findContributionsByCommitmentId(UUID commitmentId);
    List<CapitalContribution> findContributionsByBranchId(UUID branchId);

    // Profit terms operations
    ProfitEntitlementTerm saveProfitTerm(ProfitEntitlementTerm term);
    Optional<ProfitEntitlementTerm> findProfitTermById(UUID id);
    List<ProfitEntitlementTerm> findProfitTermsByAgreementId(UUID agreementId);
    List<ProfitEntitlementTerm> findProfitTermsByBranchId(UUID branchId);
}
