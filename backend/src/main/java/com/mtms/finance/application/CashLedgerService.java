package com.mtms.finance.application;

import com.mtms.evidence.application.EvidenceTokenService;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.CashLedgerPageResponse;
import com.mtms.finance.application.dto.InternalTransferCommand;
import com.mtms.finance.application.dto.InternalTransferResponseDto;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.DuplicateLedgerPostingException;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FinancialAccount;
import com.mtms.finance.domain.FinancialAccountNotFoundException;
import com.mtms.finance.domain.FinancialCategoryNotFoundException;
import com.mtms.finance.domain.InternalTransfer;
import com.mtms.finance.domain.InvalidTransferAccountsException;
import com.mtms.finance.domain.LedgerStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.InternalTransferJpaEntity;
import com.mtms.finance.infrastructure.persistence.InternalTransferJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CashLedgerService {

    private final FinancialAccountJpaRepository accountRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final CashLedgerEntryJpaRepository ledgerEntryRepository;
    private final InternalTransferJpaRepository internalTransferRepository;
    private final EvidenceTokenService evidenceTokenService;

    public CashLedgerService(
            FinancialAccountJpaRepository accountRepository,
            FinancialCategoryJpaRepository categoryRepository,
            CashLedgerEntryJpaRepository ledgerEntryRepository,
            InternalTransferJpaRepository internalTransferRepository,
            @Autowired(required = false) EvidenceTokenService evidenceTokenService
    ) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.internalTransferRepository = internalTransferRepository;
        this.evidenceTokenService = evidenceTokenService;
    }

    /**
     * Posts a ledger entry with pessimistic row locking and balance recalculation.
     * Enforces TC-011 idempotency on (account_id, source_type, source_id, source_leg).
     */
    public CashLedgerEntryDto postLedgerEntry(UUID branchId, PostLedgerEntryCommand command, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        // 1. Lock account row with pessimistic write lock
        FinancialAccountJpaEntity accountEntity = accountRepository.findByIdForUpdate(command.accountId())
                .filter(a -> branchId.equals(a.getBranchId()))
                .orElseThrow(() -> new FinancialAccountNotFoundException(
                        "Không tìm thấy tài khoản tài chính " + command.accountId() + " thuộc chi nhánh " + branchId));

        // 2. Validate category
        FinancialCategoryJpaEntity categoryEntity = categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new FinancialCategoryNotFoundException(command.categoryId()));

        // 3. Resolve source details
        SourceType sourceType = command.sourceType() != null ? command.sourceType() : resolveDefaultSourceType(categoryEntity.getCategoryGroup());
        UUID sourceId = command.sourceId() != null ? command.sourceId() : UUID.randomUUID();
        String sourceLeg = (command.sourceLeg() != null && !command.sourceLeg().isBlank()) ? command.sourceLeg() : "MAIN";

        // 4. TC-011 Idempotency Check
        if (ledgerEntryRepository.existsByAccountIdAndSourceTypeAndSourceIdAndSourceLeg(
                accountEntity.getId(), sourceType, sourceId, sourceLeg)) {
            throw new DuplicateLedgerPostingException(String.format(
                    "Bút toán cho tài khoản %s với nguồn %s/%s/%s đã được ghi nhận trước đó (TC-011)",
                    accountEntity.getId(), sourceType, sourceId, sourceLeg));
        }

        // 5. Update account balance through domain logic (handles insufficient balance check for OUTFLOW)
        FinancialAccount account = accountEntity.toDomain();
        if (command.entryType() == EntryType.INFLOW) {
            account.credit(command.amount());
        } else {
            account.debit(command.amount());
        }
        accountEntity.setCurrentBalance(account.getCurrentBalance());
        accountRepository.save(accountEntity);

        // 6. Compute signed amount & running balance snapshot
        BigDecimal signedAmount = command.entryType() == EntryType.INFLOW ?
                command.amount() : command.amount().negate();
        BigDecimal runningBalance = account.getCurrentBalance();

        // 7. Save append-only cash ledger entry
        CashLedgerEntryJpaEntity entryEntity = new CashLedgerEntryJpaEntity(
                UUID.randomUUID(),
                accountEntity.getId(),
                branchId,
                command.businessDate(),
                command.entryType(),
                command.amount(),
                signedAmount,
                runningBalance,
                categoryEntity.getId(),
                sourceType,
                sourceId,
                sourceLeg,
                LedgerStatus.POSTED,
                null,
                command.memo(),
                command.evidenceFileId(),
                actorId,
                Instant.now()
        );

        try {
            CashLedgerEntryJpaEntity saved = ledgerEntryRepository.saveAndFlush(entryEntity);
            return toDto(saved, accountEntity.getAccountName(), categoryEntity);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateLedgerPostingException(
                    "Bút toán đã tồn tại vi phạm ràng buộc toàn vẹn duy nhất (TC-011)", ex);
        }
    }

    /**
     * Executes internal transfer between two accounts of the same branch (TC-010).
     * Atomically locks both accounts in deterministic order, updates balances, and creates
     * dual SOURCE and DESTINATION cash ledger entries with zero net P&L effect.
     */
    public InternalTransferResponseDto recordInternalTransfer(
            UUID branchId,
            InternalTransferCommand command,
            UUID currentUserId
    ) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        if (command.fromAccountId().equals(command.toAccountId())) {
            throw new InvalidTransferAccountsException("Tài khoản nguồn và tài khoản đích phải khác nhau");
        }

        // Lock accounts in deterministic order to prevent deadlocks
        UUID firstId = command.fromAccountId().compareTo(command.toAccountId()) < 0 ?
                command.fromAccountId() : command.toAccountId();
        UUID secondId = command.fromAccountId().compareTo(command.toAccountId()) < 0 ?
                command.toAccountId() : command.fromAccountId();

        FinancialAccountJpaEntity firstLocked = accountRepository.findByIdForUpdate(firstId)
                .filter(a -> branchId.equals(a.getBranchId()))
                .orElseThrow(() -> new FinancialAccountNotFoundException("Không tìm thấy tài khoản " + firstId + " thuộc chi nhánh"));

        FinancialAccountJpaEntity secondLocked = accountRepository.findByIdForUpdate(secondId)
                .filter(a -> branchId.equals(a.getBranchId()))
                .orElseThrow(() -> new FinancialAccountNotFoundException("Không tìm thấy tài khoản " + secondId + " thuộc chi nhánh"));

        FinancialAccountJpaEntity fromAccount = command.fromAccountId().equals(firstId) ? firstLocked : secondLocked;
        FinancialAccountJpaEntity toAccount = command.toAccountId().equals(firstId) ? firstLocked : secondLocked;

        // Fetch TRF_INTERNAL category
        FinancialCategoryJpaEntity trfCategory = categoryRepository.findByCategoryCode("TRF_INTERNAL")
                .orElseThrow(() -> new FinancialCategoryNotFoundException("Không tìm thấy danh mục hệ thống TRF_INTERNAL"));

        // Domain balance updates
        FinancialAccount fromDomain = fromAccount.toDomain();
        FinancialAccount toDomain = toAccount.toDomain();

        fromDomain.debit(command.amount()); // Throws InsufficientAccountBalanceException if insufficient
        toDomain.credit(command.amount());

        fromAccount.setCurrentBalance(fromDomain.getCurrentBalance());
        toAccount.setCurrentBalance(toDomain.getCurrentBalance());

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Record Internal Transfer
        UUID transferId = UUID.randomUUID();
        InternalTransfer transfer = new InternalTransfer(
                transferId,
                branchId,
                command.fromAccountId(),
                command.toAccountId(),
                command.amount(),
                command.businessDate(),
                "COMPLETED",
                command.note(),
                command.evidenceFileId(),
                actorId,
                Instant.now()
        );
        internalTransferRepository.save(InternalTransferJpaEntity.fromDomain(transfer));

        // Create Source Leg entry (OUTFLOW)
        String outMemo = "Chuyển tiền nội bộ đến " + toAccount.getAccountName() +
                (command.note() != null && !command.note().isBlank() ? " (" + command.note() + ")" : "");
        CashLedgerEntryJpaEntity sourceEntry = new CashLedgerEntryJpaEntity(
                UUID.randomUUID(),
                fromAccount.getId(),
                branchId,
                command.businessDate(),
                EntryType.OUTFLOW,
                command.amount(),
                command.amount().negate(),
                fromDomain.getCurrentBalance(),
                trfCategory.getId(),
                SourceType.INTERNAL_TRANSFER,
                transferId,
                "SOURCE",
                LedgerStatus.POSTED,
                null,
                outMemo,
                command.evidenceFileId(),
                actorId,
                Instant.now()
        );
        ledgerEntryRepository.save(sourceEntry);

        // Create Destination Leg entry (INFLOW)
        String inMemo = "Nhận tiền nội bộ từ " + fromAccount.getAccountName() +
                (command.note() != null && !command.note().isBlank() ? " (" + command.note() + ")" : "");
        CashLedgerEntryJpaEntity destEntry = new CashLedgerEntryJpaEntity(
                UUID.randomUUID(),
                toAccount.getId(),
                branchId,
                command.businessDate(),
                EntryType.INFLOW,
                command.amount(),
                command.amount(),
                toDomain.getCurrentBalance(),
                trfCategory.getId(),
                SourceType.INTERNAL_TRANSFER,
                transferId,
                "DESTINATION",
                LedgerStatus.POSTED,
                null,
                inMemo,
                command.evidenceFileId(),
                actorId,
                Instant.now()
        );
        ledgerEntryRepository.save(destEntry);

        return new InternalTransferResponseDto(
                transferId,
                branchId,
                fromAccount.getId(),
                toAccount.getId(),
                command.amount(),
                command.businessDate(),
                "COMPLETED",
                command.note(),
                command.evidenceFileId(),
                sourceEntry.getId(),
                destEntry.getId(),
                actorId,
                transfer.getCreatedAt()
        );
    }

    /**
     * Tra cứu sổ quỹ tiền mặt và tài khoản ngân hàng chi nhánh (FE-P0-11).
     */
    @Transactional(readOnly = true)
    public CashLedgerPageResponse getLedgerEntries(
            UUID branchId,
            UUID accountId,
            UUID categoryId,
            SourceType sourceType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Page<CashLedgerEntryJpaEntity> page = ledgerEntryRepository.findEntries(
                branchId,
                accountId,
                categoryId,
                sourceType,
                startDate,
                endDate,
                pageable
        );

        // Preload accounts and categories in memory to avoid N+1 queries
        Map<UUID, String> accountNames = accountRepository.findByBranchIdOrderByCreatedAtAsc(branchId).stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, FinancialAccountJpaEntity::getAccountName));

        Map<UUID, FinancialCategoryJpaEntity> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(FinancialCategoryJpaEntity::getId, c -> c));

        var dtos = page.getContent().stream().map(entry -> {
            String accName = accountNames.getOrDefault(entry.getAccountId(), "Tài khoản chi nhánh");
            FinancialCategoryJpaEntity cat = categories.get(entry.getCategoryId());
            return toDto(entry, accName, cat);
        }).toList();

        return new CashLedgerPageResponse(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private SourceType resolveDefaultSourceType(CategoryGroup group) {
        return switch (group) {
            case CAPITAL_FLOW -> SourceType.CAPITAL_FLOW;
            case OPERATING_EXPENSE -> SourceType.MANUAL_EXPENSE;
            case DEBT_SETTLEMENT -> SourceType.SUPPLIER_PAYMENT;
            case PAYROLL -> SourceType.PAYROLL_PAYMENT;
            case REVENUE -> SourceType.DAILY_REVENUE;
            case INTERNAL_TRANSFER -> SourceType.INTERNAL_TRANSFER;
            default -> SourceType.MANUAL_ADJUSTMENT;
        };
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
