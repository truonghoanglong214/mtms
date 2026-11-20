package com.mtms.capital.application;

import com.mtms.capital.application.dto.CapitalDtos.*;
import static com.mtms.capital.application.dto.CapitalDtos.G5_PROVISIONAL_DISCLAIMER;
import com.mtms.capital.domain.CapitalExceptions.*;
import com.mtms.capital.domain.model.*;
import com.mtms.capital.domain.port.CapitalRepository;
import com.mtms.finance.application.CashLedgerService;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CapitalService {

    private final CapitalRepository capitalRepository;
    private final CashLedgerService cashLedgerService;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final FinancialAccountJpaRepository accountRepository;

    public CapitalService(CapitalRepository capitalRepository,
                          CashLedgerService cashLedgerService,
                          FinancialCategoryJpaRepository categoryRepository,
                          FinancialAccountJpaRepository accountRepository) {
        this.capitalRepository = capitalRepository;
        this.cashLedgerService = cashLedgerService;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    // 1. Investor Management
    public InvestorDto registerInvestor(UUID entityId, CreateInvestorRequest req) {
        Objects.requireNonNull(entityId, "Entity ID must not be null");
        Objects.requireNonNull(req, "Request must not be null");

        capitalRepository.findInvestorByEntityAndDisplayName(entityId, req.displayName().trim())
                .ifPresent(existing -> {
                    throw new InvestorConflictException(req.displayName().trim());
                });

        Investor investor = Investor.create(
                entityId,
                req.displayName().trim(),
                req.contactPhone(),
                req.contactEmail(),
                req.identityNumber(),
                req.contactNote()
        );

        Investor saved = capitalRepository.saveInvestor(investor);
        return toInvestorDto(saved);
    }

    @Transactional(readOnly = true)
    public List<InvestorDto> getInvestorsByEntity(UUID entityId) {
        return capitalRepository.findInvestorsByEntityId(entityId).stream()
                .map(this::toInvestorDto)
                .collect(Collectors.toList());
    }

    // 2. Agreement Management
    public AgreementDto createAgreement(UUID branchId, CreateAgreementRequest req) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(req, "Request must not be null");

        Investor investor = capitalRepository.findInvestorById(req.investorId())
                .orElseThrow(() -> new InvestorNotFoundException(req.investorId()));

        capitalRepository.findAgreementByBranchAndRef(branchId, req.agreementRef().trim())
                .ifPresent(existing -> {
                    throw new AgreementConflictException(req.agreementRef().trim());
                });

        if (req.effectiveTo() != null && req.effectiveTo().isBefore(req.effectiveFrom())) {
            throw new InvalidContributionException("Ngày kết thúc hiệu lực không được trước ngày bắt đầu");
        }

        InvestorAgreement agreement = InvestorAgreement.create(
                investor.getId(),
                branchId,
                req.agreementRef().trim(),
                req.contributionType(),
                req.effectiveFrom(),
                req.effectiveTo(),
                req.eligibilityTerms(),
                req.evidenceFileId()
        );

        InvestorAgreement saved = capitalRepository.saveAgreement(agreement);
        return toAgreementDto(saved, investor.getDisplayName());
    }

    @Transactional(readOnly = true)
    public List<AgreementDto> getAgreementsByBranch(UUID branchId) {
        List<InvestorAgreement> agreements = capitalRepository.findAgreementsByBranchId(branchId);
        return agreements.stream()
                .map(a -> {
                    String invName = capitalRepository.findInvestorById(a.getInvestorId())
                            .map(Investor::getDisplayName)
                            .orElse("N/A");
                    return toAgreementDto(a, invName);
                })
                .collect(Collectors.toList());
    }

    // 3. Commitment Management
    public CommitmentDto createCommitment(UUID branchId, CreateCommitmentRequest req) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(req, "Request must not be null");

        InvestorAgreement agreement = capitalRepository.findAgreementById(req.agreementId())
                .orElseThrow(() -> new AgreementNotFoundException(req.agreementId()));

        if (!agreement.getBranchId().equals(branchId)) {
            throw new AgreementNotFoundException(req.agreementId());
        }

        if (req.committedValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidContributionException("Giá trị cam kết không được âm");
        }

        BigDecimal nonCash = req.recognizedNonCashValue() != null ? req.recognizedNonCashValue() : BigDecimal.ZERO;
        if (nonCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidContributionException("Giá trị phi tiền mặt không được âm");
        }

        CapitalCommitment commitment = CapitalCommitment.create(
                agreement.getId(),
                req.committedValue(),
                nonCash,
                req.commitmentDueDate(),
                req.notes()
        );

        CapitalCommitment saved = capitalRepository.saveCommitment(commitment);
        String invName = capitalRepository.findInvestorById(agreement.getInvestorId())
                .map(Investor::getDisplayName)
                .orElse("N/A");
        return toCommitmentDto(saved, agreement, invName, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<CommitmentDto> getCommitmentsByBranch(UUID branchId) {
        List<CapitalCommitment> commitments = capitalRepository.findCommitmentsByBranchId(branchId);
        Map<UUID, InvestorAgreement> agreementMap = capitalRepository.findAgreementsByBranchId(branchId).stream()
                .collect(Collectors.toMap(InvestorAgreement::getId, a -> a));

        return commitments.stream()
                .map(c -> {
                    InvestorAgreement agreement = agreementMap.get(c.getAgreementId());
                    String invName = "N/A";
                    if (agreement != null) {
                        invName = capitalRepository.findInvestorById(agreement.getInvestorId())
                                .map(Investor::getDisplayName)
                                .orElse("N/A");
                    }
                    List<CapitalContribution> contributions = capitalRepository.findContributionsByCommitmentId(c.getId());
                    BigDecimal actual = contributions.stream()
                            .map(CapitalContribution::getRecognizedValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return toCommitmentDto(c, agreement, invName, actual);
                })
                .collect(Collectors.toList());
    }

    // 4. Contribution Management & TC-011 Cash Ledger Linkage
    public ContributionDto recordContribution(UUID branchId, UUID commitmentId,
                                              RecordContributionRequest req, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(commitmentId, "Commitment ID must not be null");
        Objects.requireNonNull(req, "Request must not be null");

        CapitalCommitment commitment = capitalRepository.findCommitmentById(commitmentId)
                .orElseThrow(() -> new CommitmentNotFoundException(commitmentId));

        InvestorAgreement agreement = capitalRepository.findAgreementById(commitment.getAgreementId())
                .orElseThrow(() -> new AgreementNotFoundException(commitment.getAgreementId()));

        if (!agreement.getBranchId().equals(branchId)) {
            throw new CommitmentNotFoundException(commitmentId);
        }

        if (req.recognizedValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidContributionException("Giá trị góp vốn phải lớn hơn 0");
        }

        UUID ledgerEntryId = null;
        String accountName = null;

        if (req.contributionType() == ContributionType.CASH) {
            if (req.accountId() == null) {
                throw new InvalidContributionException("Góp vốn bằng tiền mặt hoặc ngân hàng bắt buộc phải chọn tài khoản quỹ");
            }

            FinancialAccountJpaEntity account = accountRepository.findById(req.accountId())
                    .orElseThrow(() -> new InvalidContributionException("Tài khoản tài chính không tồn tại: " + req.accountId()));
            accountName = account.getAccountName();

            // Link to Cash Ledger under CAPITAL_FLOW / CAP_INJECTION
            FinancialCategoryJpaEntity category = categoryRepository.findByCategoryCode("CAP_INJECTION")
                    .orElseGet(() -> categoryRepository.findAll().stream()
                            .filter(c -> c.getCategoryGroup() == CategoryGroup.CAPITAL_FLOW)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("CAP_INJECTION category not found")));

            String memo = "Vốn góp cổ đông (" + agreement.getAgreementRef() + "): " +
                    (req.notes() != null && !req.notes().isBlank() ? req.notes() : "Đóng góp vốn kinh doanh");

            PostLedgerEntryCommand postCmd = new PostLedgerEntryCommand(
                    req.accountId(),
                    category.getId(),
                    EntryType.INFLOW,
                    req.recognizedValue(),
                    req.contributedOn(),
                    memo,
                    req.evidenceFileId(),
                    SourceType.CAPITAL_FLOW,
                    commitmentId,
                    "CAPITAL_CONTRIBUTION"
            );

            CashLedgerEntryDto ledgerEntry = cashLedgerService.postLedgerEntry(branchId, postCmd, currentUserId);
            ledgerEntryId = ledgerEntry.id();
        }

        CapitalContribution contribution = CapitalContribution.create(
                commitment.getId(),
                req.contributedOn(),
                req.contributionType(),
                req.recognizedValue(),
                req.accountId(),
                req.assetId(),
                ledgerEntryId,
                req.evidenceFileId(),
                req.notes(),
                currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001")
        );

        CapitalContribution saved = capitalRepository.saveContribution(contribution);

        // Update commitment status
        List<CapitalContribution> allContributions = capitalRepository.findContributionsByCommitmentId(commitmentId);
        BigDecimal totalContributed = allContributions.stream()
                .map(CapitalContribution::getRecognizedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalContributed.compareTo(commitment.getCommittedValue()) >= 0) {
            commitment.setStatus(CommitmentStatus.FULFILLED);
        } else if (totalContributed.compareTo(BigDecimal.ZERO) > 0) {
            commitment.setStatus(CommitmentStatus.PARTIALLY_FULFILLED);
        } else {
            commitment.setStatus(CommitmentStatus.PENDING);
        }
        capitalRepository.saveCommitment(commitment);

        return toContributionDto(saved, accountName);
    }

    @Transactional(readOnly = true)
    public List<ContributionDto> getContributionsByBranch(UUID branchId) {
        List<CapitalContribution> contributions = capitalRepository.findContributionsByBranchId(branchId);
        return contributions.stream()
                .map(c -> {
                    String accName = null;
                    if (c.getAccountId() != null) {
                        accName = accountRepository.findById(c.getAccountId())
                                .map(FinancialAccountJpaEntity::getAccountName)
                                .orElse(null);
                    }
                    return toContributionDto(c, accName);
                })
                .collect(Collectors.toList());
    }

    // 5. Profit Entitlement Terms Management (CAP-INV-01 & G5 Gate)
    public ProfitTermDto configureProfitTerms(UUID branchId, CreateProfitTermRequest req, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(req, "Request must not be null");

        InvestorAgreement agreement = capitalRepository.findAgreementById(req.agreementId())
                .orElseThrow(() -> new AgreementNotFoundException(req.agreementId()));

        if (!agreement.getBranchId().equals(branchId)) {
            throw new AgreementNotFoundException(req.agreementId());
        }

        if (req.profitShareRatio().compareTo(BigDecimal.ZERO) < 0 ||
            req.profitShareRatio().compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidProfitTermException("Tỷ lệ chia lợi nhuận phải nằm trong khoảng 0.0 đến 1.0 (0% đến 100%)");
        }

        if (req.effectiveTo() != null && req.effectiveTo().isBefore(req.effectiveFrom())) {
            throw new InvalidProfitTermException("Ngày kết thúc hiệu lực không được trước ngày bắt đầu");
        }

        ProfitEntitlementTerm term = ProfitEntitlementTerm.create(
                agreement.getId(),
                req.effectiveFrom(),
                req.effectiveTo(),
                req.profitShareRatio(),
                req.eligibilityConditions(),
                currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001"),
                req.evidenceFileId()
        );

        ProfitEntitlementTerm saved = capitalRepository.saveProfitTerm(term);
        String invName = capitalRepository.findInvestorById(agreement.getInvestorId())
                .map(Investor::getDisplayName)
                .orElse("N/A");

        return toProfitTermDto(saved, agreement, invName);
    }

    @Transactional(readOnly = true)
    public List<ProfitTermDto> getProfitTermsByBranch(UUID branchId) {
        List<ProfitEntitlementTerm> terms = capitalRepository.findProfitTermsByBranchId(branchId);
        Map<UUID, InvestorAgreement> agreementMap = capitalRepository.findAgreementsByBranchId(branchId).stream()
                .collect(Collectors.toMap(InvestorAgreement::getId, a -> a));

        return terms.stream()
                .map(t -> {
                    InvestorAgreement agreement = agreementMap.get(t.getAgreementId());
                    String invName = "N/A";
                    if (agreement != null) {
                        invName = capitalRepository.findInvestorById(agreement.getInvestorId())
                                .map(Investor::getDisplayName)
                                .orElse("N/A");
                    }
                    return toProfitTermDto(t, agreement, invName);
                })
                .collect(Collectors.toList());
    }

    // 6. Capital Summary & TC-032 / CAP-INV-01 Reconciliation
    @Transactional(readOnly = true)
    public CapitalSummaryDto getCapitalSummary(UUID branchId) {
        List<InvestorAgreement> agreements = capitalRepository.findAgreementsByBranchId(branchId);
        List<CapitalCommitment> commitments = capitalRepository.findCommitmentsByBranchId(branchId);
        List<CapitalContribution> contributions = capitalRepository.findContributionsByBranchId(branchId);
        List<ProfitEntitlementTerm> profitTerms = capitalRepository.findProfitTermsByBranchId(branchId);

        BigDecimal totalCommitted = commitments.stream()
                .map(CapitalCommitment::getCommittedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalContributed = contributions.stream()
                .map(CapitalContribution::getRecognizedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = totalCommitted.subtract(totalContributed);
        if (totalOutstanding.compareTo(BigDecimal.ZERO) < 0) {
            totalOutstanding = BigDecimal.ZERO;
        }

        // Group by Investor to compute TC-032 & CAP-INV-01 breakdown
        Set<UUID> uniqueInvestorIds = agreements.stream()
                .map(InvestorAgreement::getInvestorId)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        List<InvestorCapitalBreakdownDto> breakdowns = new ArrayList<>();

        for (UUID investorId : uniqueInvestorIds) {
            String investorName = capitalRepository.findInvestorById(investorId)
                    .map(Investor::getDisplayName)
                    .orElse("N/A");

            List<UUID> investorAgreementIds = agreements.stream()
                    .filter(a -> a.getInvestorId().equals(investorId))
                    .map(InvestorAgreement::getId)
                    .toList();

            BigDecimal invCommitted = commitments.stream()
                    .filter(c -> investorAgreementIds.contains(c.getAgreementId()))
                    .map(CapitalCommitment::getCommittedValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<UUID> investorCommitmentIds = commitments.stream()
                    .filter(c -> investorAgreementIds.contains(c.getAgreementId()))
                    .map(CapitalCommitment::getId)
                    .toList();

            BigDecimal invContributed = contributions.stream()
                    .filter(cb -> investorCommitmentIds.contains(cb.getCommitmentId()))
                    .map(CapitalContribution::getRecognizedValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal invOutstanding = invCommitted.subtract(invContributed);
            if (invOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                invOutstanding = BigDecimal.ZERO;
            }

            // Capital share percentage
            BigDecimal capitalSharePercent = BigDecimal.ZERO;
            if (totalContributed.compareTo(BigDecimal.ZERO) > 0) {
                capitalSharePercent = invContributed.multiply(BigDecimal.valueOf(100))
                        .divide(totalContributed, 2, RoundingMode.HALF_UP);
            }

            // Active Profit share ratio for today (latest effective term)
            Optional<ProfitEntitlementTerm> activeTerm = profitTerms.stream()
                    .filter(pt -> investorAgreementIds.contains(pt.getAgreementId()))
                    .filter(pt -> !pt.getEffectiveFrom().isAfter(today) &&
                                  (pt.getEffectiveTo() == null || !pt.getEffectiveTo().isBefore(today)))
                    .max(Comparator.comparing(ProfitEntitlementTerm::getEffectiveFrom));

            BigDecimal profitSharePercent = activeTerm
                    .map(pt -> pt.getProfitShareRatio().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .orElse(BigDecimal.ZERO);

            boolean isDifferent = capitalSharePercent.subtract(profitSharePercent).abs().compareTo(BigDecimal.valueOf(0.01)) > 0;

            breakdowns.add(new InvestorCapitalBreakdownDto(
                    investorId,
                    investorName,
                    invCommitted,
                    invContributed,
                    invOutstanding,
                    capitalSharePercent,
                    profitSharePercent,
                    isDifferent
            ));
        }

        return new CapitalSummaryDto(
                branchId,
                totalCommitted,
                totalContributed,
                totalOutstanding,
                uniqueInvestorIds.size(),
                agreements.size(),
                true,
                G5_PROVISIONAL_DISCLAIMER,
                breakdowns
        );
    }

    // Helper Mappers
    private InvestorDto toInvestorDto(Investor i) {
        return new InvestorDto(
                i.getId(),
                i.getEntityId(),
                i.getDisplayName(),
                i.getContactPhone(),
                i.getContactEmail(),
                i.getIdentityNumber(),
                i.getContactNote(),
                i.isActive(),
                i.getCreatedAt()
        );
    }

    private AgreementDto toAgreementDto(InvestorAgreement a, String investorName) {
        return new AgreementDto(
                a.getId(),
                a.getInvestorId(),
                investorName,
                a.getBranchId(),
                a.getAgreementRef(),
                a.getContributionType(),
                a.getEffectiveFrom(),
                a.getEffectiveTo(),
                a.getStatus(),
                a.getEligibilityTerms(),
                a.getEvidenceFileId(),
                a.getCreatedAt()
        );
    }

    private CommitmentDto toCommitmentDto(CapitalCommitment c, InvestorAgreement agreement,
                                          String investorName, BigDecimal actualContributed) {
        BigDecimal outstanding = c.getCommittedValue().subtract(actualContributed);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }

        return new CommitmentDto(
                c.getId(),
                c.getAgreementId(),
                agreement != null ? agreement.getInvestorId() : null,
                investorName,
                agreement != null ? agreement.getAgreementRef() : "N/A",
                c.getCommittedValue(),
                c.getRecognizedNonCashValue(),
                actualContributed,
                outstanding,
                c.getCommitmentDueDate(),
                c.getStatus(),
                c.getNotes(),
                c.getCreatedAt()
        );
    }

    private ContributionDto toContributionDto(CapitalContribution cb, String accountName) {
        return new ContributionDto(
                cb.getId(),
                cb.getCommitmentId(),
                cb.getContributedOn(),
                cb.getContributionType(),
                cb.getRecognizedValue(),
                cb.getAccountId(),
                accountName,
                cb.getAssetId(),
                cb.getLedgerEntryId(),
                cb.getEvidenceFileId(),
                cb.getNotes(),
                cb.getCreatedBy(),
                cb.getCreatedAt()
        );
    }

    private ProfitTermDto toProfitTermDto(ProfitEntitlementTerm pt, InvestorAgreement agreement, String investorName) {
        BigDecimal percent = pt.getProfitShareRatio().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return new ProfitTermDto(
                pt.getId(),
                pt.getAgreementId(),
                agreement != null ? agreement.getInvestorId() : null,
                investorName,
                agreement != null ? agreement.getAgreementRef() : "N/A",
                pt.getEffectiveFrom(),
                pt.getEffectiveTo(),
                pt.getProfitShareRatio(),
                percent,
                pt.getEligibilityConditions(),
                pt.getApprovedBy(),
                pt.getEvidenceFileId(),
                pt.getCreatedAt(),
                true,
                G5_PROVISIONAL_DISCLAIMER
        );
    }
}
