package com.mtms.finance.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.evidence.application.EvidenceTokenService;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.CreateOpeningBatchCommand;
import com.mtms.finance.application.dto.OpeningAccountBalanceDto;
import com.mtms.finance.application.dto.OpeningBalanceBatchDto;
import com.mtms.finance.application.dto.OpeningBalanceStatusResponse;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FinancialAccountNotFoundException;
import com.mtms.finance.domain.FinancialCategoryNotFoundException;
import com.mtms.finance.domain.InvalidCutoverDateException;
import com.mtms.finance.domain.OpeningBatchAlreadyPostedException;
import com.mtms.finance.domain.OpeningBatchNotFoundException;
import com.mtms.finance.domain.OpeningBatchStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.EntityTaxObligationJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.OpeningAccountBalanceJpaEntity;
import com.mtms.finance.infrastructure.persistence.OpeningAccountBalanceJpaRepository;
import com.mtms.finance.infrastructure.persistence.OpeningBalanceBatchJpaEntity;
import com.mtms.finance.infrastructure.persistence.OpeningBalanceBatchJpaRepository;
import com.mtms.finance.infrastructure.persistence.ReceivableJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.inventory.infrastructure.persistence.OpeningInventoryRecordJpaEntity;
import com.mtms.inventory.infrastructure.persistence.OpeningInventoryRecordJpaRepository;
import com.mtms.inventory.infrastructure.persistence.PayableJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009
 * Application service for opening balances import & cutover batch.
 */
@Service
@Transactional
public class OpeningBalanceService {

    private final OpeningBalanceBatchJpaRepository batchRepository;
    private final OpeningAccountBalanceJpaRepository openingAccountBalanceRepository;
    private final FinancialAccountJpaRepository accountRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final CashLedgerService cashLedgerService;
    private final OpeningInventoryRecordJpaRepository openingInventoryRepository;
    private final PayableJpaRepository payableRepository;
    private final ReceivableJpaRepository receivableRepository;
    private final EntityTaxObligationJpaRepository taxObligationRepository;
    private final UserJpaRepository userRepository;
    private final EvidenceTokenService evidenceTokenService;
    private final ObjectMapper objectMapper;

    public OpeningBalanceService(
            OpeningBalanceBatchJpaRepository batchRepository,
            OpeningAccountBalanceJpaRepository openingAccountBalanceRepository,
            FinancialAccountJpaRepository accountRepository,
            FinancialCategoryJpaRepository categoryRepository,
            CashLedgerService cashLedgerService,
            OpeningInventoryRecordJpaRepository openingInventoryRepository,
            PayableJpaRepository payableRepository,
            ReceivableJpaRepository receivableRepository,
            EntityTaxObligationJpaRepository taxObligationRepository,
            UserJpaRepository userRepository,
            @Autowired(required = false) EvidenceTokenService evidenceTokenService,
            ObjectMapper objectMapper
    ) {
        this.batchRepository = batchRepository;
        this.openingAccountBalanceRepository = openingAccountBalanceRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.cashLedgerService = cashLedgerService;
        this.openingInventoryRepository = openingInventoryRepository;
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.taxObligationRepository = taxObligationRepository;
        this.userRepository = userRepository;
        this.evidenceTokenService = evidenceTokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * Checks if opening balances have been posted for the branch and returns the active batch if present.
     */
    @Transactional(readOnly = true)
    public OpeningBalanceStatusResponse getOpeningStatus(UUID branchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Optional<OpeningBalanceBatchJpaEntity> batchOpt = batchRepository.findByBranchId(branchId);
        if (batchOpt.isEmpty()) {
            return new OpeningBalanceStatusResponse(false, null, null);
        }
        OpeningBalanceBatchJpaEntity batch = batchOpt.get();
        boolean isPosted = batch.getStatus() == OpeningBatchStatus.POSTED;
        LocalDate cutoverDate = batch.getCutoverDate();
        OpeningBalanceBatchDto dto = toDto(batch);
        return new OpeningBalanceStatusResponse(isPosted, cutoverDate, dto);
    }

    /**
     * Retrieves detail of an opening balance batch.
     */
    @Transactional(readOnly = true)
    public OpeningBalanceBatchDto getOpeningBatchDetail(UUID branchId, UUID batchId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(batchId, "Batch ID must not be null");
        OpeningBalanceBatchJpaEntity batch = batchRepository.findByIdAndBranchId(batchId, branchId)
                .orElseThrow(() -> new OpeningBatchNotFoundException("Không tìm thấy kỳ chốt số dư đầu kỳ ID " + batchId));
        return toDto(batch);
    }

    /**
     * Creates or updates a draft opening balance batch with working capital reconciliation.
     */
    public OpeningBalanceBatchDto saveOpeningBatch(UUID branchId, CreateOpeningBatchCommand cmd, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(cmd, "Command must not be null");
        if (cmd.cutoverDate() == null) {
            throw new InvalidCutoverDateException("Ngày chốt số dư đầu kỳ không được để trống");
        }
        UUID actorId = currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        Optional<OpeningBalanceBatchJpaEntity> existingOpt = batchRepository.findByBranchId(branchId);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == OpeningBatchStatus.POSTED) {
            throw new OpeningBatchAlreadyPostedException("Chi nhánh đã chốt số dư đầu kỳ, không thể chỉnh sửa");
        }

        // 1. Validate accounts and calculate totalCash & totalBank
        List<FinancialAccountJpaEntity> accounts = accountRepository.findByBranchIdOrderByCreatedAtAsc(branchId);
        Map<UUID, FinancialAccountJpaEntity> accountMap = accounts.stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, a -> a));

        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalBank = BigDecimal.ZERO;

        if (cmd.accountBalances() != null) {
            for (CreateOpeningBatchCommand.AccountBalanceItem item : cmd.accountBalances()) {
                FinancialAccountJpaEntity acc = accountMap.get(item.accountId());
                if (acc == null) {
                    throw new FinancialAccountNotFoundException("Tài khoản " + item.accountId() + " không thuộc chi nhánh " + branchId);
                }
                if (item.openingBalance() != null) {
                    if (acc.getAccountKind() == AccountKind.CASH) {
                        totalCash = totalCash.add(item.openingBalance());
                    } else if (acc.getAccountKind() == AccountKind.BANK) {
                        totalBank = totalBank.add(item.openingBalance());
                    }
                }
            }
        }

        // 2. Read opening inventory valuation
        BigDecimal totalInventory = openingInventoryRepository.findByBranchId(branchId)
                .map(OpeningInventoryRecordJpaEntity::getTotalValue)
                .orElse(BigDecimal.ZERO);

        // 3. Read opening payables
        BigDecimal totalPayable = payableRepository.sumOutstandingDebtByBranchId(branchId);
        if (totalPayable == null) {
            totalPayable = BigDecimal.ZERO;
        }

        // 4. Read opening receivables
        BigDecimal totalReceivable = receivableRepository.sumOutstandingReceivablesByBranchId(branchId);
        if (totalReceivable == null) {
            totalReceivable = BigDecimal.ZERO;
        }

        // 5. Read opening tax liabilities
        BigDecimal totalTax = taxObligationRepository.sumOutstandingTaxByBranchId(branchId);
        if (totalTax == null) {
            totalTax = BigDecimal.ZERO;
        }

        // 6. Net working capital = (totalCash + totalBank + totalInventory + totalReceivable) - (totalPayable + totalTax)
        BigDecimal netWorkingCapital = totalCash.add(totalBank).add(totalInventory).add(totalReceivable)
                .subtract(totalPayable.add(totalTax));

        // 7. Snapshot json
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("cutoverDate", cmd.cutoverDate().toString());
        snapshot.put("totalCashAmount", totalCash);
        snapshot.put("totalBankAmount", totalBank);
        snapshot.put("totalInventoryValuation", totalInventory);
        snapshot.put("totalPayableAmount", totalPayable);
        snapshot.put("totalReceivableAmount", totalReceivable);
        snapshot.put("totalTaxLiabilityAmount", totalTax);
        snapshot.put("netWorkingCapital", netWorkingCapital);

        String snapshotJson = "{}";
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception ignored) {
        }

        // 8. Create or update batch
        OpeningBalanceBatchJpaEntity batch;
        if (existingOpt.isPresent()) {
            batch = existingOpt.get();
            batch.setCutoverDate(cmd.cutoverDate());
            batch.setTotalCashAmount(totalCash);
            batch.setTotalBankAmount(totalBank);
            batch.setTotalInventoryValuation(totalInventory);
            batch.setTotalPayableAmount(totalPayable);
            batch.setTotalReceivableAmount(totalReceivable);
            batch.setTotalTaxLiabilityAmount(totalTax);
            batch.setNetWorkingCapital(netWorkingCapital);
            batch.setSummarySnapshot(snapshotJson);
            batch.setNote(cmd.note());
            batch.setEvidenceFileId(cmd.evidenceFileId());
            openingAccountBalanceRepository.deleteByBatchId(batch.getId());
        } else {
            batch = new OpeningBalanceBatchJpaEntity(
                    UUID.randomUUID(),
                    branchId,
                    cmd.cutoverDate(),
                    OpeningBatchStatus.DRAFT,
                    totalCash,
                    totalBank,
                    totalInventory,
                    totalPayable,
                    totalReceivable,
                    totalTax,
                    netWorkingCapital,
                    snapshotJson,
                    cmd.note(),
                    cmd.evidenceFileId(),
                    actorId,
                    null,
                    null,
                    Instant.now()
            );
        }

        OpeningBalanceBatchJpaEntity savedBatch = batchRepository.save(batch);

        // 9. Save account balances
        if (cmd.accountBalances() != null) {
            for (CreateOpeningBatchCommand.AccountBalanceItem item : cmd.accountBalances()) {
                OpeningAccountBalanceJpaEntity accEntity = new OpeningAccountBalanceJpaEntity(
                        UUID.randomUUID(),
                        savedBatch.getId(),
                        item.accountId(),
                        item.openingBalance() != null ? item.openingBalance() : BigDecimal.ZERO,
                        null
                );
                openingAccountBalanceRepository.save(accEntity);
            }
        }

        return toDto(savedBatch);
    }

    /**
     * Posts opening balance batch atomically:
     * - Inflows opening cash/bank balances into mtms.cash_ledger_entries with category OPENING_BALANCE.
     * - Updates account balances.
     * - Transitions batch status to POSTED.
     */
    public OpeningBalanceBatchDto postOpeningBatch(UUID branchId, UUID batchId, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(batchId, "Batch ID must not be null");
        UUID actorId = currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        OpeningBalanceBatchJpaEntity batch = batchRepository.findByIdForUpdate(batchId)
                .filter(b -> branchId.equals(b.getBranchId()))
                .orElseThrow(() -> new OpeningBatchNotFoundException("Không tìm thấy kỳ chốt số dư đầu kỳ ID " + batchId));

        if (batch.getStatus() == OpeningBatchStatus.POSTED) {
            throw new OpeningBatchAlreadyPostedException("Kỳ chốt số dư đầu kỳ đã được ghi sổ trước đó");
        }

        FinancialCategoryJpaEntity category = categoryRepository.findByCategoryCode("OPENING_BALANCE")
                .orElseThrow(() -> new FinancialCategoryNotFoundException("Không tìm thấy danh mục hệ thống OPENING_BALANCE"));

        List<OpeningAccountBalanceJpaEntity> accountBalances = openingAccountBalanceRepository.findByBatchId(batch.getId());

        for (OpeningAccountBalanceJpaEntity accBalance : accountBalances) {
            if (accBalance.getOpeningBalance() != null && accBalance.getOpeningBalance().compareTo(BigDecimal.ZERO) > 0) {
                PostLedgerEntryCommand postCmd = new PostLedgerEntryCommand(
                        accBalance.getAccountId(),
                        category.getId(),
                        EntryType.INFLOW,
                        accBalance.getOpeningBalance(),
                        batch.getCutoverDate(),
                        "Số dư tiền đầu kỳ ngày " + batch.getCutoverDate(),
                        batch.getEvidenceFileId(),
                        SourceType.CAPITAL_FLOW,
                        batch.getId(),
                        "OPENING_BALANCE"
                );
                CashLedgerEntryDto entryDto = cashLedgerService.postLedgerEntry(branchId, postCmd, actorId);
                accBalance.setCashLedgerEntryId(entryDto.id());
                openingAccountBalanceRepository.save(accBalance);
            }
        }

        batch.setStatus(OpeningBatchStatus.POSTED);
        batch.setPostedBy(actorId);
        batch.setPostedAt(Instant.now());
        OpeningBalanceBatchJpaEntity saved = batchRepository.save(batch);

        return toDto(saved);
    }

    private OpeningBalanceBatchDto toDto(OpeningBalanceBatchJpaEntity entity) {
        List<OpeningAccountBalanceJpaEntity> items = openingAccountBalanceRepository.findByBatchId(entity.getId());
        List<FinancialAccountJpaEntity> accounts = accountRepository.findByBranchIdOrderByCreatedAtAsc(entity.getBranchId());
        Map<UUID, FinancialAccountJpaEntity> accountMap = accounts.stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, a -> a));

        List<OpeningAccountBalanceDto> accountDtos = items.stream().map(item -> {
            FinancialAccountJpaEntity acc = accountMap.get(item.getAccountId());
            String accName = acc != null ? acc.getAccountName() : null;
            String accKind = acc != null && acc.getAccountKind() != null ? acc.getAccountKind().name() : null;
            return new OpeningAccountBalanceDto(
                    item.getId(),
                    item.getAccountId(),
                    accName,
                    accKind,
                    item.getOpeningBalance(),
                    item.getCashLedgerEntryId()
            );
        }).toList();

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName, (k1, k2) -> k1));

        String createdByName = userNames.get(entity.getCreatedBy());
        String postedByName = entity.getPostedBy() != null ? userNames.get(entity.getPostedBy()) : null;
        String evidenceDownloadUrl = resolveEvidenceToken(entity.getEvidenceFileId(), entity.getBranchId());

        return new OpeningBalanceBatchDto(
                entity.getId(),
                entity.getBranchId(),
                entity.getCutoverDate(),
                entity.getStatus(),
                entity.getTotalCashAmount(),
                entity.getTotalBankAmount(),
                entity.getTotalInventoryValuation(),
                entity.getTotalPayableAmount(),
                entity.getTotalReceivableAmount(),
                entity.getTotalTaxLiabilityAmount(),
                entity.getNetWorkingCapital(),
                entity.getSummarySnapshot(),
                entity.getNote(),
                entity.getEvidenceFileId(),
                evidenceDownloadUrl,
                entity.getCreatedBy(),
                createdByName,
                entity.getPostedBy(),
                postedByName,
                entity.getPostedAt(),
                entity.getCreatedAt(),
                accountDtos
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
}
