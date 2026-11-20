package com.mtms.finance.application;

import com.mtms.evidence.application.EvidenceTokenService;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FinancialAccount;
import com.mtms.finance.domain.FinancialAccountNotFoundException;
import com.mtms.finance.domain.FinancialCategory;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.LedgerStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-REV-05 / BR-REV-04 / TC-007 / TC-011
 * Synchronous linkage service creating and reversing cash/bank ledger entries
 * upon daily revenue report confirmation and revision adjustments.
 */
@Service
@Transactional
public class RevenueCashLinkageService {

    private final FinancialAccountJpaRepository accountRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final CashLedgerEntryJpaRepository ledgerEntryRepository;
    private final FinancialAccountService financialAccountService;
    private final EvidenceTokenService evidenceTokenService;

    public RevenueCashLinkageService(
            FinancialAccountJpaRepository accountRepository,
            FinancialCategoryJpaRepository categoryRepository,
            CashLedgerEntryJpaRepository ledgerEntryRepository,
            FinancialAccountService financialAccountService,
            @Autowired(required = false) EvidenceTokenService evidenceTokenService
    ) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.financialAccountService = financialAccountService;
        this.evidenceTokenService = evidenceTokenService;
    }

    /**
     * Synchronously links confirmed daily revenue to branch cash and bank accounts.
     * Enforces TC-007 (delivery app revenue isolation) and TC-011 (idempotency).
     */
    public void linkDailyRevenue(
            UUID branchId,
            UUID reportId,
            LocalDate businessDate,
            BigDecimal cashAmount,
            BigDecimal bankTransferAmount,
            UUID supersededReportId,
            UUID actorUserId
    ) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(reportId, "Report ID must not be null");
        Objects.requireNonNull(businessDate, "Business date must not be null");

        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        // 1. If this is a revision of an earlier report, reverse the superseded report's active entries
        if (supersededReportId != null) {
            reverseSupersededReportEntries(branchId, supersededReportId);
        }

        // 2. Post Cash Leg if cashAmount > 0
        if (cashAmount != null && cashAmount.compareTo(BigDecimal.ZERO) > 0) {
            postRevenueLeg(
                    branchId,
                    reportId,
                    businessDate,
                    AccountKind.CASH,
                    "CASH_LEG",
                    "REV_CASH",
                    "Doanh thu bán hàng - Tiền mặt",
                    cashAmount,
                    "Doanh thu tiền mặt ngày " + businessDate,
                    actorId
            );
        }

        // 3. Post Bank Leg if bankTransferAmount > 0
        if (bankTransferAmount != null && bankTransferAmount.compareTo(BigDecimal.ZERO) > 0) {
            postRevenueLeg(
                    branchId,
                    reportId,
                    businessDate,
                    AccountKind.BANK,
                    "BANK_LEG",
                    "REV_BANK",
                    "Doanh thu bán hàng - Chuyển khoản",
                    bankTransferAmount,
                    "Doanh thu chuyển khoản ngày " + businessDate,
                    actorId
            );
        }

        // TC-007 Invariant: NO ledger entries for deliveryAppNetAmount.
        // Bank and Cash accounts are strictly untouched by delivery app net revenue.
    }

    /**
     * Retrieves all ledger entries linked to a daily revenue report.
     */
    @Transactional(readOnly = true)
    public List<CashLedgerEntryDto> getRevenueLedgerEntries(UUID branchId, UUID reportId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(reportId, "Report ID must not be null");

        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository
                .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(
                        branchId, SourceType.DAILY_REVENUE, reportId);

        if (entries.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> accountNames = accountRepository.findByBranchIdOrderByCreatedAtAsc(branchId).stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, FinancialAccountJpaEntity::getAccountName));

        Map<UUID, FinancialCategoryJpaEntity> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(FinancialCategoryJpaEntity::getId, c -> c));

        return entries.stream().map(entry -> {
            String accName = accountNames.getOrDefault(entry.getAccountId(), "Tài khoản chi nhánh");
            FinancialCategoryJpaEntity cat = categories.get(entry.getCategoryId());
            return toDto(entry, accName, cat);
        }).toList();
    }

    private void reverseSupersededReportEntries(UUID branchId, UUID supersededReportId) {
        List<CashLedgerEntryJpaEntity> oldEntries = ledgerEntryRepository
                .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(
                        branchId, SourceType.DAILY_REVENUE, supersededReportId);

        for (CashLedgerEntryJpaEntity oldEntry : oldEntries) {
            if (oldEntry.getStatus() == LedgerStatus.POSTED) {
                // Lock account and reverse balance
                FinancialAccountJpaEntity accountEntity = accountRepository.findByIdForUpdate(oldEntry.getAccountId())
                        .filter(a -> branchId.equals(a.getBranchId()))
                        .orElseThrow(() -> new FinancialAccountNotFoundException(
                                "Không tìm thấy tài khoản tài chính " + oldEntry.getAccountId() + " thuộc chi nhánh " + branchId));

                FinancialAccount accountDomain = accountEntity.toDomain();
                if (oldEntry.getEntryType() == EntryType.INFLOW) {
                    accountDomain.debit(oldEntry.getAmount());
                } else {
                    accountDomain.credit(oldEntry.getAmount());
                }
                accountEntity.setCurrentBalance(accountDomain.getCurrentBalance());
                accountRepository.save(accountEntity);

                oldEntry.setStatus(LedgerStatus.REVERSED);
                ledgerEntryRepository.save(oldEntry);
            }
        }
    }

    private void postRevenueLeg(
            UUID branchId,
            UUID reportId,
            LocalDate businessDate,
            AccountKind accountKind,
            String sourceLeg,
            String categoryCode,
            String defaultCategoryName,
            BigDecimal amount,
            String memo,
            UUID actorId
    ) {
        // 1. Ensure financial account for branch
        FinancialAccountJpaEntity accountEntity = accountKind == AccountKind.CASH
                ? financialAccountService.ensureDefaultCashAccount(branchId)
                : financialAccountService.ensureDefaultBankAccount(branchId);

        // 2. TC-011 Idempotency check: if entry already exists, do not duplicate
        if (ledgerEntryRepository.existsByAccountIdAndSourceTypeAndSourceIdAndSourceLeg(
                accountEntity.getId(), SourceType.DAILY_REVENUE, reportId, sourceLeg)) {
            return;
        }

        // 3. Pessimistically lock account for balance update
        accountEntity = accountRepository.findByIdForUpdate(accountEntity.getId())
                .filter(a -> branchId.equals(a.getBranchId()))
                .orElse(accountEntity);

        // 4. Ensure category exists
        FinancialCategoryJpaEntity category = getOrCreateRevenueCategory(categoryCode, defaultCategoryName);

        // 5. Update account balance
        FinancialAccount domainAccount = accountEntity.toDomain();
        domainAccount.credit(amount);
        accountEntity.setCurrentBalance(domainAccount.getCurrentBalance());
        accountRepository.save(accountEntity);

        // 6. Record immutable cash ledger entry
        CashLedgerEntryJpaEntity entry = new CashLedgerEntryJpaEntity(
                UUID.randomUUID(),
                accountEntity.getId(),
                branchId,
                businessDate,
                EntryType.INFLOW,
                amount,
                amount,
                domainAccount.getCurrentBalance(),
                category.getId(),
                SourceType.DAILY_REVENUE,
                reportId,
                sourceLeg,
                LedgerStatus.POSTED,
                null,
                memo,
                null,
                actorId,
                Instant.now()
        );

        try {
            ledgerEntryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            // Concurrently written entry, idempotent handling
        }
    }

    private FinancialCategoryJpaEntity getOrCreateRevenueCategory(String code, String defaultName) {
        return categoryRepository.findByCategoryCode(code).orElseGet(() -> {
            FinancialCategory category = new FinancialCategory(
                    UUID.randomUUID(),
                    code,
                    defaultName,
                    CategoryGroup.REVENUE,
                    FlowDirection.INFLOW,
                    true,
                    true,
                    defaultName,
                    Instant.now()
            );
            return categoryRepository.save(FinancialCategoryJpaEntity.fromDomain(category));
        });
    }

    private CashLedgerEntryDto toDto(
            CashLedgerEntryJpaEntity entry,
            String accountName,
            FinancialCategoryJpaEntity category
    ) {
        String token = null;
        if (entry.getEvidenceFileId() != null && evidenceTokenService != null) {
            try {
                token = evidenceTokenService.generateToken(entry.getEvidenceFileId(), entry.getBranchId());
            } catch (Exception ignored) {
            }
        }

        String catCode = category != null ? category.getCategoryCode() : "N/A";
        String catName = category != null ? category.getCategoryName() : "Không xác định";
        CategoryGroup catGroup = category != null ? category.getCategoryGroup() : CategoryGroup.OTHER;

        return new CashLedgerEntryDto(
                entry.getId(),
                entry.getAccountId(),
                accountName,
                entry.getBranchId(),
                entry.getBusinessDate(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getSignedAmount(),
                entry.getRunningBalance(),
                entry.getCategoryId(),
                catCode,
                catName,
                catGroup,
                entry.getSourceType(),
                entry.getSourceId(),
                entry.getSourceLeg(),
                entry.getStatus(),
                entry.getReversalOfId(),
                entry.getMemo(),
                entry.getEvidenceFileId(),
                token,
                entry.getCreatedBy(),
                entry.getPostedAt()
        );
    }
}
