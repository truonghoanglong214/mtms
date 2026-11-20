package com.mtms.capital.infrastructure.persistence;

import com.mtms.capital.domain.model.CapitalCommitment;
import com.mtms.capital.domain.model.CapitalContribution;
import com.mtms.capital.domain.model.Investor;
import com.mtms.capital.domain.model.InvestorAgreement;
import com.mtms.capital.domain.model.ProfitEntitlementTerm;
import com.mtms.capital.domain.port.CapitalRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CapitalRepositoryAdapter implements CapitalRepository {

    private final SpringDataInvestorRepository investorRepo;
    private final SpringDataInvestorAgreementRepository agreementRepo;
    private final SpringDataCapitalCommitmentRepository commitmentRepo;
    private final SpringDataCapitalContributionRepository contributionRepo;
    private final SpringDataProfitEntitlementTermRepository profitTermRepo;

    public CapitalRepositoryAdapter(SpringDataInvestorRepository investorRepo,
                                  SpringDataInvestorAgreementRepository agreementRepo,
                                  SpringDataCapitalCommitmentRepository commitmentRepo,
                                  SpringDataCapitalContributionRepository contributionRepo,
                                  SpringDataProfitEntitlementTermRepository profitTermRepo) {
        this.investorRepo = investorRepo;
        this.agreementRepo = agreementRepo;
        this.commitmentRepo = commitmentRepo;
        this.contributionRepo = contributionRepo;
        this.profitTermRepo = profitTermRepo;
    }

    // Investor
    @Override
    public Investor saveInvestor(Investor d) {
        InvestorJpaEntity e = toEntity(d);
        InvestorJpaEntity saved = investorRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<Investor> findInvestorById(UUID id) {
        return investorRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Investor> findInvestorByEntityAndDisplayName(UUID entityId, String displayName) {
        return investorRepo.findByEntityIdAndDisplayName(entityId, displayName).map(this::toDomain);
    }

    @Override
    public List<Investor> findInvestorsByEntityId(UUID entityId) {
        return investorRepo.findByEntityId(entityId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    // Agreement
    @Override
    public InvestorAgreement saveAgreement(InvestorAgreement d) {
        InvestorAgreementJpaEntity e = toEntity(d);
        InvestorAgreementJpaEntity saved = agreementRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<InvestorAgreement> findAgreementById(UUID id) {
        return agreementRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<InvestorAgreement> findAgreementByBranchAndRef(UUID branchId, String agreementRef) {
        return agreementRepo.findByBranchIdAndAgreementRef(branchId, agreementRef).map(this::toDomain);
    }

    @Override
    public List<InvestorAgreement> findAgreementsByBranchId(UUID branchId) {
        return agreementRepo.findByBranchId(branchId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InvestorAgreement> findAgreementsByInvestorId(UUID investorId) {
        return agreementRepo.findByInvestorId(investorId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    // Commitment
    @Override
    public CapitalCommitment saveCommitment(CapitalCommitment d) {
        CapitalCommitmentJpaEntity e = toEntity(d);
        CapitalCommitmentJpaEntity saved = commitmentRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<CapitalCommitment> findCommitmentById(UUID id) {
        return commitmentRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<CapitalCommitment> findCommitmentsByAgreementId(UUID agreementId) {
        return commitmentRepo.findByAgreementId(agreementId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CapitalCommitment> findCommitmentsByBranchId(UUID branchId) {
        return commitmentRepo.findByBranchId(branchId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    // Contribution
    @Override
    public CapitalContribution saveContribution(CapitalContribution d) {
        CapitalContributionJpaEntity e = toEntity(d);
        CapitalContributionJpaEntity saved = contributionRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<CapitalContribution> findContributionById(UUID id) {
        return contributionRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<CapitalContribution> findContributionsByCommitmentId(UUID commitmentId) {
        return contributionRepo.findByCommitmentId(commitmentId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CapitalContribution> findContributionsByBranchId(UUID branchId) {
        return contributionRepo.findByBranchId(branchId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    // Profit Term
    @Override
    public ProfitEntitlementTerm saveProfitTerm(ProfitEntitlementTerm d) {
        ProfitEntitlementTermJpaEntity e = toEntity(d);
        ProfitEntitlementTermJpaEntity saved = profitTermRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<ProfitEntitlementTerm> findProfitTermById(UUID id) {
        return profitTermRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProfitEntitlementTerm> findProfitTermsByAgreementId(UUID agreementId) {
        return profitTermRepo.findByAgreementId(agreementId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ProfitEntitlementTerm> findProfitTermsByBranchId(UUID branchId) {
        return profitTermRepo.findByBranchId(branchId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    // Mappers
    private InvestorJpaEntity toEntity(Investor d) {
        InvestorJpaEntity e = new InvestorJpaEntity();
        e.setId(d.getId());
        e.setEntityId(d.getEntityId());
        e.setDisplayName(d.getDisplayName());
        e.setContactPhone(d.getContactPhone());
        e.setContactEmail(d.getContactEmail());
        e.setIdentityNumber(d.getIdentityNumber());
        e.setContactNote(d.getContactNote());
        e.setActive(d.isActive());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    private Investor toDomain(InvestorJpaEntity e) {
        return new Investor(e.getId(), e.getEntityId(), e.getDisplayName(), e.getContactPhone(),
                e.getContactEmail(), e.getIdentityNumber(), e.getContactNote(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private InvestorAgreementJpaEntity toEntity(InvestorAgreement d) {
        InvestorAgreementJpaEntity e = new InvestorAgreementJpaEntity();
        e.setId(d.getId());
        e.setInvestorId(d.getInvestorId());
        e.setBranchId(d.getBranchId());
        e.setAgreementRef(d.getAgreementRef());
        e.setContributionType(d.getContributionType());
        e.setEffectiveFrom(d.getEffectiveFrom());
        e.setEffectiveTo(d.getEffectiveTo());
        e.setStatus(d.getStatus());
        e.setEligibilityTerms(d.getEligibilityTerms());
        e.setEvidenceFileId(d.getEvidenceFileId());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    private InvestorAgreement toDomain(InvestorAgreementJpaEntity e) {
        return new InvestorAgreement(e.getId(), e.getInvestorId(), e.getBranchId(), e.getAgreementRef(),
                e.getContributionType(), e.getEffectiveFrom(), e.getEffectiveTo(), e.getStatus(),
                e.getEligibilityTerms(), e.getEvidenceFileId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private CapitalCommitmentJpaEntity toEntity(CapitalCommitment d) {
        CapitalCommitmentJpaEntity e = new CapitalCommitmentJpaEntity();
        e.setId(d.getId());
        e.setAgreementId(d.getAgreementId());
        e.setCommittedValue(d.getCommittedValue());
        e.setRecognizedNonCashValue(d.getRecognizedNonCashValue());
        e.setCommitmentDueDate(d.getCommitmentDueDate());
        e.setStatus(d.getStatus());
        e.setNotes(d.getNotes());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    private CapitalCommitment toDomain(CapitalCommitmentJpaEntity e) {
        return new CapitalCommitment(e.getId(), e.getAgreementId(), e.getCommittedValue(),
                e.getRecognizedNonCashValue(), e.getCommitmentDueDate(), e.getStatus(), e.getNotes(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private CapitalContributionJpaEntity toEntity(CapitalContribution d) {
        CapitalContributionJpaEntity e = new CapitalContributionJpaEntity();
        e.setId(d.getId());
        e.setCommitmentId(d.getCommitmentId());
        e.setContributedOn(d.getContributedOn());
        e.setContributionType(d.getContributionType());
        e.setRecognizedValue(d.getRecognizedValue());
        e.setAccountId(d.getAccountId());
        e.setAssetId(d.getAssetId());
        e.setLedgerEntryId(d.getLedgerEntryId());
        e.setEvidenceFileId(d.getEvidenceFileId());
        e.setNotes(d.getNotes());
        e.setCreatedBy(d.getCreatedBy());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    private CapitalContribution toDomain(CapitalContributionJpaEntity e) {
        return new CapitalContribution(e.getId(), e.getCommitmentId(), e.getContributedOn(),
                e.getContributionType(), e.getRecognizedValue(), e.getAccountId(), e.getAssetId(),
                e.getLedgerEntryId(), e.getEvidenceFileId(), e.getNotes(), e.getCreatedBy(), e.getCreatedAt());
    }

    private ProfitEntitlementTermJpaEntity toEntity(ProfitEntitlementTerm d) {
        ProfitEntitlementTermJpaEntity e = new ProfitEntitlementTermJpaEntity();
        e.setId(d.getId());
        e.setAgreementId(d.getAgreementId());
        e.setEffectiveFrom(d.getEffectiveFrom());
        e.setEffectiveTo(d.getEffectiveTo());
        e.setProfitShareRatio(d.getProfitShareRatio());
        e.setEligibilityConditions(d.getEligibilityConditions());
        e.setApprovedBy(d.getApprovedBy());
        e.setEvidenceFileId(d.getEvidenceFileId());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    private ProfitEntitlementTerm toDomain(ProfitEntitlementTermJpaEntity e) {
        return new ProfitEntitlementTerm(e.getId(), e.getAgreementId(), e.getEffectiveFrom(),
                e.getEffectiveTo(), e.getProfitShareRatio(), e.getEligibilityConditions(),
                e.getApprovedBy(), e.getEvidenceFileId(), e.getCreatedAt());
    }
}
