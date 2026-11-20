package com.mtms.finance.application;

import com.mtms.evidence.application.EvidenceTokenService;
import com.mtms.finance.application.dto.BranchExpenseConfigDto;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.CreateExpenseCommand;
import com.mtms.finance.application.dto.ExpenseDetailDto;
import com.mtms.finance.application.dto.ExpenseDto;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.application.dto.UpdateExpenseConfigCommand;
import com.mtms.finance.application.dto.VoidExpenseCommand;
import com.mtms.finance.domain.BranchExpenseConfig;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.ExpenseAlreadyConfirmedException;
import com.mtms.finance.domain.ExpenseAlreadyVoidedException;
import com.mtms.finance.domain.ExpenseNotFoundException;
import com.mtms.finance.domain.ExpenseStatus;
import com.mtms.finance.domain.FinancialAccount;
import com.mtms.finance.domain.FinancialAccountNotFoundException;
import com.mtms.finance.domain.FinancialCategoryNotFoundException;
import com.mtms.finance.domain.InvalidExpenseCategoryException;
import com.mtms.finance.domain.LedgerStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.BranchExpenseConfigJpaEntity;
import com.mtms.finance.infrastructure.persistence.BranchExpenseConfigJpaRepository;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.ExpenseJpaEntity;
import com.mtms.finance.infrastructure.persistence.ExpenseJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.inventory.domain.PayableStatus;
import com.mtms.inventory.infrastructure.persistence.PayableJpaEntity;
import com.mtms.inventory.infrastructure.persistence.PayableJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * P0-FIN-03 / P0-FIN-04 / BR-FIN-07 / TC-011
 * Application service for operating expense entry, approval lifecycle, and ledger postings.
 */
@Service
@Transactional
public class ExpenseService {

    private final ExpenseJpaRepository expenseRepository;
    private final BranchExpenseConfigJpaRepository configRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final FinancialAccountJpaRepository accountRepository;
    private final CashLedgerService cashLedgerService;
    private final CashLedgerEntryJpaRepository ledgerEntryRepository;
    private final BranchJpaRepository branchRepository;
    private final UserJpaRepository userRepository;
    private final PayableJpaRepository payableRepository;
    private final EvidenceTokenService evidenceTokenService;

    public ExpenseService(
            ExpenseJpaRepository expenseRepository,
            BranchExpenseConfigJpaRepository configRepository,
            FinancialCategoryJpaRepository categoryRepository,
            FinancialAccountJpaRepository accountRepository,
            CashLedgerService cashLedgerService,
            CashLedgerEntryJpaRepository ledgerEntryRepository,
            BranchJpaRepository branchRepository,
            UserJpaRepository userRepository,
            PayableJpaRepository payableRepository,
            @Autowired(required = false) EvidenceTokenService evidenceTokenService
    ) {
        this.expenseRepository = expenseRepository;
        this.configRepository = configRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.cashLedgerService = cashLedgerService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.payableRepository = payableRepository;
        this.evidenceTokenService = evidenceTokenService;
    }

    /**
     * Retrieves paginated list of expenses for a branch with optional filters.
     */
    @Transactional(readOnly = true)
    public Page<ExpenseDto> getExpenses(
            UUID branchId,
            ExpenseStatus status,
            UUID categoryId,
            LocalDate startDate,
            LocalDate endDate,
            Boolean reviewFlagOnly,
            String search,
            Pageable pageable
    ) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");

        Page<ExpenseJpaEntity> page = expenseRepository.findExpenses(
                branchId,
                status,
                categoryId,
                startDate,
                endDate,
                reviewFlagOnly,
                (search != null && !search.isBlank()) ? search.trim() : null,
                pageable
        );

        Map<UUID, FinancialCategoryJpaEntity> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(FinancialCategoryJpaEntity::getId, c -> c));

        Map<UUID, String> accountNames = accountRepository.findByBranchIdOrderByCreatedAtAsc(branchId).stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, FinancialAccountJpaEntity::getAccountName));

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName));

        return page.map(e -> toDto(e, categories.get(e.getCategoryId()), accountNames.get(e.getPaymentAccountId()), userNames));
    }

    /**
     * Retrieves detailed information of a single expense including linked cash ledger entry.
     */
    @Transactional(readOnly = true)
    public ExpenseDetailDto getExpenseDetail(UUID branchId, UUID expenseId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(expenseId, "Expense ID must not be null");

        ExpenseJpaEntity entity = expenseRepository.findByBranchIdAndId(branchId, expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));

        FinancialCategoryJpaEntity category = categoryRepository.findById(entity.getCategoryId()).orElse(null);
        String accountName = null;
        if (entity.getPaymentAccountId() != null) {
            accountName = accountRepository.findById(entity.getPaymentAccountId())
                    .map(FinancialAccountJpaEntity::getAccountName)
                    .orElse(null);
        }

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName));

        CashLedgerEntryDto linkedEntryDto = null;
        if (entity.isPaid() && entity.getStatus() == ExpenseStatus.CONFIRMED) {
            List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository
                    .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(
                            branchId, SourceType.MANUAL_EXPENSE, entity.getId());
            if (!entries.isEmpty()) {
                CashLedgerEntryJpaEntity lastEntry = entries.get(entries.size() - 1);
                linkedEntryDto = toCashLedgerEntryDto(lastEntry, accountName, category);
            }
        }

        String evidenceUrl = resolveEvidenceToken(entity.getEvidenceFileId(), branchId);

        return new ExpenseDetailDto(
                entity.getId(),
                entity.getBranchId(),
                entity.getExpenseCode(),
                entity.getExpenseDate(),
                entity.getCategoryId(),
                category != null ? category.getCategoryCode() : "N/A",
                category != null ? category.getCategoryName() : "Không xác định",
                entity.getAmount(),
                entity.isPaid(),
                entity.getPaymentAccountId(),
                accountName,
                entity.getPayableId(),
                entity.getEvidenceFileId(),
                evidenceUrl,
                entity.getStatus(),
                entity.isReviewFlag(),
                entity.getReason(),
                entity.getCreatedBy(),
                userNames.getOrDefault(entity.getCreatedBy(), "Hệ thống"),
                entity.getConfirmedBy(),
                entity.getConfirmedBy() != null ? userNames.getOrDefault(entity.getConfirmedBy(), "Hệ thống") : null,
                entity.getConfirmedAt(),
                entity.getVoidedBy(),
                entity.getVoidedBy() != null ? userNames.getOrDefault(entity.getVoidedBy(), "Hệ thống") : null,
                entity.getVoidedAt(),
                entity.getVoidReason(),
                entity.getCreatedAt(),
                linkedEntryDto
        );
    }

    /**
     * Creates an operating expense record.
     */
    public ExpenseDto createExpense(UUID branchId, CreateExpenseCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

        FinancialCategoryJpaEntity category = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new FinancialCategoryNotFoundException(command.categoryId()));

        // 1. Invariant: Only OPERATING_EXPENSE category group allowed
        if (category.getCategoryGroup() != CategoryGroup.OPERATING_EXPENSE) {
            throw new InvalidExpenseCategoryException(
                    "Danh mục chi phí phải thuộc nhóm OPERATING_EXPENSE: " + category.getCategoryGroup());
        }

        // 2. Validate payment account if paid immediately
        String accountName = null;
        if (Boolean.TRUE.equals(command.isPaid())) {
            if (command.paymentAccountId() == null) {
                throw new IllegalArgumentException("Tài khoản thanh toán là bắt buộc khi chi phí đã thanh toán ngay");
            }
            FinancialAccountJpaEntity account = accountRepository.findById(command.paymentAccountId())
                    .filter(a -> branchId.equals(a.getBranchId()))
                    .orElseThrow(() -> new FinancialAccountNotFoundException(
                            "Không tìm thấy tài khoản thanh toán " + command.paymentAccountId() + " thuộc chi nhánh " + branchId));
            accountName = account.getAccountName();
        }

        // 3. Alert threshold check
        BranchExpenseConfigJpaEntity config = configRepository.findByBranchId(branchId).orElse(null);
        BigDecimal threshold = config != null ? config.getAlertThresholdAmount() : BranchExpenseConfig.DEFAULT_ALERT_THRESHOLD;
        boolean reviewFlag = command.amount().compareTo(threshold) >= 0;

        // 4. Generate expense code: EXP-BR{code}-{YYYYMM}-{seq}
        int year = command.expenseDate().getYear();
        int month = command.expenseDate().getMonthValue();
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        long count = expenseRepository.countByBranchIdAndExpenseDateBetween(branchId, startOfMonth, endOfMonth);

        String rawCode = branch.getBranchCode().trim();
        String branchCodePart = rawCode.startsWith("BR") ? rawCode : "BR" + rawCode;
        String expenseCode = String.format("EXP-%s-%04d%02d-%03d", branchCodePart, year, month, count + 1);
        int seq = 1;
        while (expenseRepository.existsByBranchIdAndExpenseCode(branchId, expenseCode)) {
            expenseCode = String.format("EXP-%s-%04d%02d-%03d", branchCodePart, year, month, count + 1 + seq);
            seq++;
        }

        ExpenseJpaEntity entity = new ExpenseJpaEntity(
                UUID.randomUUID(),
                branchId,
                expenseCode,
                command.expenseDate(),
                category.getId(),
                command.amount(),
                Boolean.TRUE.equals(command.isPaid()),
                command.paymentAccountId(),
                null,
                command.evidenceFileId(),
                ExpenseStatus.DRAFT,
                reviewFlag,
                command.reason().trim(),
                actorId,
                null,
                null,
                null,
                null,
                null,
                Instant.now()
        );

        ExpenseJpaEntity saved = expenseRepository.saveAndFlush(entity);

        if (command.confirmImmediately()) {
            return confirmExpense(branchId, saved.getId(), actorId);
        }

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName));

        return toDto(saved, category, accountName, userNames);
    }

    /**
     * Confirms an operating expense and posts to cash ledger or payables.
     */
    public ExpenseDto confirmExpense(UUID branchId, UUID expenseId, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(expenseId, "Expense ID must not be null");
        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        ExpenseJpaEntity entity = expenseRepository.findByIdAndBranchIdForUpdate(branchId, expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));

        if (entity.getStatus() == ExpenseStatus.CONFIRMED) {
            throw new ExpenseAlreadyConfirmedException("Phiếu chi đã được xác nhận trước đó: " + entity.getExpenseCode());
        }
        if (entity.getStatus() == ExpenseStatus.VOIDED) {
            throw new ExpenseAlreadyVoidedException("Không thể xác nhận phiếu chi đã bị hủy: " + entity.getExpenseCode());
        }

        FinancialCategoryJpaEntity category = categoryRepository.findById(entity.getCategoryId())
                .orElseThrow(() -> new FinancialCategoryNotFoundException(entity.getCategoryId()));

        String accountName = null;
        if (entity.isPaid()) {
            // Mode A: Paid immediately -> post OUTFLOW to Cash/Bank Ledger
            PostLedgerEntryCommand postCommand = new PostLedgerEntryCommand(
                    entity.getPaymentAccountId(),
                    entity.getCategoryId(),
                    EntryType.OUTFLOW,
                    entity.getAmount(),
                    entity.getExpenseDate(),
                    "Chi phí: " + category.getCategoryName() + " - " + entity.getReason(),
                    entity.getEvidenceFileId(),
                    SourceType.MANUAL_EXPENSE,
                    entity.getId(),
                    "MAIN"
            );
            CashLedgerEntryDto ledgerDto = cashLedgerService.postLedgerEntry(branchId, postCommand, actorId);
            accountName = ledgerDto.accountName();
        } else {
            // Mode B: Unpaid / on credit -> create open payable liability
            BranchJpaEntity branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

            PayableJpaEntity payable = new PayableJpaEntity(
                    UUID.randomUUID(),
                    branch.getBusinessEntityId(),
                    branchId,
                    entity.getReason(),
                    "MANUAL_PAYABLE",
                    entity.getId(),
                    entity.getExpenseDate(),
                    null,
                    entity.getAmount(),
                    BigDecimal.ZERO,
                    PayableStatus.OPEN,
                    Instant.now()
            );
            payable = payableRepository.save(payable);
            entity.setPayableId(payable.getId());
        }

        entity.setStatus(ExpenseStatus.CONFIRMED);
        entity.setConfirmedBy(actorId);
        entity.setConfirmedAt(Instant.now());

        ExpenseJpaEntity saved = expenseRepository.save(entity);

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName));

        return toDto(saved, category, accountName, userNames);
    }

    /**
     * Voids an operating expense, reversing cash ledger postings or cancelling liabilities.
     */
    public ExpenseDto voidExpense(UUID branchId, UUID expenseId, VoidExpenseCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(expenseId, "Expense ID must not be null");
        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        ExpenseJpaEntity entity = expenseRepository.findByIdAndBranchIdForUpdate(branchId, expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));

        if (entity.getStatus() == ExpenseStatus.VOIDED) {
            throw new ExpenseAlreadyVoidedException("Phiếu chi đã bị hủy trước đó: " + entity.getExpenseCode());
        }

        // Reversal of confirmed side effects
        if (entity.getStatus() == ExpenseStatus.CONFIRMED) {
            if (entity.isPaid()) {
                // Restore account balance and mark cash ledger entries REVERSED
                List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository
                        .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(
                                branchId, SourceType.MANUAL_EXPENSE, entity.getId());

                for (CashLedgerEntryJpaEntity entry : entries) {
                    if (entry.getStatus() == LedgerStatus.POSTED) {
                        FinancialAccountJpaEntity accountEntity = accountRepository.findByIdForUpdate(entry.getAccountId())
                                .filter(a -> branchId.equals(a.getBranchId()))
                                .orElseThrow(() -> new FinancialAccountNotFoundException(
                                        "Không tìm thấy tài khoản tài chính " + entry.getAccountId() + " thuộc chi nhánh " + branchId));

                        FinancialAccount accountDomain = accountEntity.toDomain();
                        if (entry.getEntryType() == EntryType.OUTFLOW) {
                            accountDomain.credit(entry.getAmount());
                        } else {
                            accountDomain.debit(entry.getAmount());
                        }
                        accountEntity.setCurrentBalance(accountDomain.getCurrentBalance());
                        accountRepository.save(accountEntity);

                        entry.setStatus(LedgerStatus.REVERSED);
                        ledgerEntryRepository.save(entry);
                    }
                }
            } else if (entity.getPayableId() != null) {
                // Cancel linked payable if unpaid
                payableRepository.findById(entity.getPayableId()).ifPresent(payable -> {
                    if (payable.getStatus() == PayableStatus.OPEN) {
                        payable.setStatus(PayableStatus.CANCELLED);
                        payableRepository.save(payable);
                    }
                });
            }
        }

        entity.setStatus(ExpenseStatus.VOIDED);
        entity.setVoidedBy(actorId);
        entity.setVoidedAt(Instant.now());
        entity.setVoidReason((command != null && command.reason() != null && !command.reason().isBlank())
                ? command.reason().trim() : "Hủy phiếu chi");

        ExpenseJpaEntity saved = expenseRepository.save(entity);

        FinancialCategoryJpaEntity category = categoryRepository.findById(entity.getCategoryId()).orElse(null);
        String accountName = entity.getPaymentAccountId() != null
                ? accountRepository.findById(entity.getPaymentAccountId()).map(FinancialAccountJpaEntity::getAccountName).orElse(null)
                : null;

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName));

        return toDto(saved, category, accountName, userNames);
    }

    /**
     * Gets branch expense threshold configuration.
     */
    @Transactional(readOnly = true)
    public BranchExpenseConfigDto getExpenseConfig(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");

        BranchExpenseConfigJpaEntity config = configRepository.findByBranchId(branchId)
                .orElseGet(() -> new BranchExpenseConfigJpaEntity(
                        branchId,
                        BranchExpenseConfig.DEFAULT_ALERT_THRESHOLD,
                        BranchExpenseConfig.DEFAULT_EVIDENCE_THRESHOLD,
                        Instant.now(),
                        Instant.now()
                ));

        return new BranchExpenseConfigDto(
                config.getBranchId(),
                config.getAlertThresholdAmount(),
                config.getEvidenceRequiredThreshold(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    /**
     * Updates branch expense threshold configuration.
     */
    public BranchExpenseConfigDto updateExpenseConfig(UUID branchId, UpdateExpenseConfigCommand command) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");

        BranchExpenseConfigJpaEntity config = configRepository.findByBranchId(branchId)
                .orElseGet(() -> new BranchExpenseConfigJpaEntity(
                        branchId,
                        command.alertThresholdAmount(),
                        command.evidenceRequiredThreshold(),
                        Instant.now(),
                        Instant.now()
                ));

        config.setAlertThresholdAmount(command.alertThresholdAmount());
        config.setEvidenceRequiredThreshold(command.evidenceRequiredThreshold());
        config.setUpdatedAt(Instant.now());

        BranchExpenseConfigJpaEntity saved = configRepository.save(config);

        return new BranchExpenseConfigDto(
                saved.getBranchId(),
                saved.getAlertThresholdAmount(),
                saved.getEvidenceRequiredThreshold(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    private String resolveEvidenceToken(UUID evidenceFileId, UUID branchId) {
        if (evidenceFileId != null && evidenceTokenService != null) {
            try {
                return evidenceTokenService.generateToken(evidenceFileId, branchId);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private ExpenseDto toDto(
            ExpenseJpaEntity entity,
            FinancialCategoryJpaEntity category,
            String accountName,
            Map<UUID, String> userNames
    ) {
        String token = resolveEvidenceToken(entity.getEvidenceFileId(), entity.getBranchId());

        return new ExpenseDto(
                entity.getId(),
                entity.getBranchId(),
                entity.getExpenseCode(),
                entity.getExpenseDate(),
                entity.getCategoryId(),
                category != null ? category.getCategoryCode() : "N/A",
                category != null ? category.getCategoryName() : "Không xác định",
                entity.getAmount(),
                entity.isPaid(),
                entity.getPaymentAccountId(),
                accountName,
                entity.getPayableId(),
                entity.getEvidenceFileId(),
                token,
                entity.getStatus(),
                entity.isReviewFlag(),
                entity.getReason(),
                entity.getCreatedBy(),
                userNames.getOrDefault(entity.getCreatedBy(), "Hệ thống"),
                entity.getConfirmedBy(),
                entity.getConfirmedBy() != null ? userNames.getOrDefault(entity.getConfirmedBy(), "Hệ thống") : null,
                entity.getConfirmedAt(),
                entity.getVoidedBy(),
                entity.getVoidedBy() != null ? userNames.getOrDefault(entity.getVoidedBy(), "Hệ thống") : null,
                entity.getVoidedAt(),
                entity.getVoidReason(),
                entity.getCreatedAt()
        );
    }

    private CashLedgerEntryDto toCashLedgerEntryDto(
            CashLedgerEntryJpaEntity entry,
            String accountName,
            FinancialCategoryJpaEntity category
    ) {
        String token = resolveEvidenceToken(entry.getEvidenceFileId(), entry.getBranchId());
        String catCode = category != null ? category.getCategoryCode() : "N/A";
        String catName = category != null ? category.getCategoryName() : "Không xác định";
        CategoryGroup catGroup = category != null ? category.getCategoryGroup() : CategoryGroup.OTHER;

        return new CashLedgerEntryDto(
                entry.getId(),
                entry.getAccountId(),
                accountName != null ? accountName : "Tài khoản chi nhánh",
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
