package com.mtms.finance.application;

import com.mtms.evidence.application.EvidenceTokenService;
import com.mtms.finance.application.dto.AdjustReconciliationCommand;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.CashReconciliationDto;
import com.mtms.finance.application.dto.CashReconciliationPageResponse;
import com.mtms.finance.application.dto.CreateReconciliationCommand;
import com.mtms.finance.application.dto.ExplainDiscrepancyCommand;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FinancialAccountNotFoundException;
import com.mtms.finance.domain.InvalidReconciliationStatusException;
import com.mtms.finance.domain.NoVarianceToAdjustException;
import com.mtms.finance.domain.ReconciliationAlreadyAdjustedException;
import com.mtms.finance.domain.ReconciliationAlreadyExistsException;
import com.mtms.finance.domain.ReconciliationDisposition;
import com.mtms.finance.domain.ReconciliationNotFoundException;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashReconciliationJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashReconciliationJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * Application service managing cash and bank account reconciliation lifecycle,
 * discrepancy tracking, and authorized synchronous cash ledger adjustments.
 */
@Service
@Transactional
public class CashReconciliationService {

    private final CashReconciliationJpaRepository reconciliationRepository;
    private final FinancialAccountJpaRepository accountRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final CashLedgerService cashLedgerService;
    private final UserJpaRepository userRepository;
    private final EvidenceTokenService evidenceTokenService;

    public CashReconciliationService(
            CashReconciliationJpaRepository reconciliationRepository,
            FinancialAccountJpaRepository accountRepository,
            FinancialCategoryJpaRepository categoryRepository,
            CashLedgerService cashLedgerService,
            UserJpaRepository userRepository,
            @Autowired(required = false) EvidenceTokenService evidenceTokenService
    ) {
        this.reconciliationRepository = reconciliationRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.cashLedgerService = cashLedgerService;
        this.userRepository = userRepository;
        this.evidenceTokenService = evidenceTokenService;
    }

    /**
     * Creates a new reconciliation snapshot for an account on a business date.
     * Computes variance = observedBalance - recordedBalance.
     * Auto-closes if variance is 0, otherwise sets status to OPEN.
     */
    public CashReconciliationDto createSnapshot(UUID branchId, CreateReconciliationCommand command, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        FinancialAccountJpaEntity account = accountRepository.findById(command.accountId())
                .filter(a -> branchId.equals(a.getBranchId()))
                .orElseThrow(() -> new FinancialAccountNotFoundException(
                        "Không tìm thấy tài khoản tài chính " + command.accountId() + " thuộc chi nhánh " + branchId));

        if (reconciliationRepository.existsByAccountIdAndReconciliationDate(command.accountId(), command.reconciliationDate())) {
            throw new ReconciliationAlreadyExistsException(command.accountId(), command.reconciliationDate());
        }

        BigDecimal recordedBalance = account.getCurrentBalance();
        BigDecimal observedBalance = command.observedBalance();
        BigDecimal varianceAmount = observedBalance.subtract(recordedBalance);

        boolean isBalanced = varianceAmount.compareTo(BigDecimal.ZERO) == 0;
        ReconciliationDisposition disposition = isBalanced ? ReconciliationDisposition.CLOSED : ReconciliationDisposition.OPEN;

        CashReconciliationJpaEntity entity = new CashReconciliationJpaEntity(
                UUID.randomUUID(),
                branchId,
                command.accountId(),
                command.reconciliationDate(),
                recordedBalance,
                observedBalance,
                varianceAmount,
                disposition,
                command.note(),
                command.evidenceFileId(),
                null,
                actorId,
                isBalanced ? actorId : null,
                isBalanced ? Instant.now() : null,
                Instant.now()
        );

        CashReconciliationJpaEntity saved = reconciliationRepository.save(entity);
        Map<UUID, String> userNames = resolveUserNames();
        return toDto(saved, account, userNames);
    }

    /**
     * Explains an open discrepancy with note and evidence attachment.
     */
    public CashReconciliationDto explainDiscrepancy(UUID branchId, UUID reconciliationId, ExplainDiscrepancyCommand command, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(reconciliationId, "Reconciliation ID must not be null");

        CashReconciliationJpaEntity reconciliation = reconciliationRepository.findByBranchIdAndId(branchId, reconciliationId)
                .orElseThrow(() -> new ReconciliationNotFoundException(reconciliationId));

        if (reconciliation.getDisposition() == ReconciliationDisposition.CLOSED
                || reconciliation.getDisposition() == ReconciliationDisposition.ADJUSTED) {
            throw new InvalidReconciliationStatusException(reconciliationId, reconciliation.getDisposition(), "giải trình chênh lệch");
        }

        reconciliation.setNote(command.note());
        if (command.evidenceFileId() != null) {
            reconciliation.setEvidenceFileId(command.evidenceFileId());
        }
        reconciliation.setDisposition(ReconciliationDisposition.EXPLAINED);

        CashReconciliationJpaEntity saved = reconciliationRepository.save(reconciliation);
        FinancialAccountJpaEntity account = accountRepository.findById(saved.getAccountId()).orElse(null);
        Map<UUID, String> userNames = resolveUserNames();
        return toDto(saved, account, userNames);
    }

    /**
     * Authorized adjustment posting to synchronize system recorded balance with observed count (TC-012).
     */
    public CashReconciliationDto adjustVariance(UUID branchId, UUID reconciliationId, AdjustReconciliationCommand command, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(reconciliationId, "Reconciliation ID must not be null");
        UUID actorId = currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        CashReconciliationJpaEntity reconciliation = reconciliationRepository.findByIdAndBranchIdForUpdate(branchId, reconciliationId)
                .orElseThrow(() -> new ReconciliationNotFoundException(reconciliationId));

        if (reconciliation.getDisposition() == ReconciliationDisposition.ADJUSTED
                || reconciliation.getDisposition() == ReconciliationDisposition.CLOSED) {
            throw new ReconciliationAlreadyAdjustedException(reconciliationId);
        }

        BigDecimal variance = reconciliation.getVarianceAmount();
        if (variance == null || variance.compareTo(BigDecimal.ZERO) == 0) {
            throw new NoVarianceToAdjustException(reconciliationId);
        }

        CashLedgerEntryDto postedEntry;
        if (variance.compareTo(BigDecimal.ZERO) > 0) {
            // Surplus -> INFLOW
            FinancialCategoryJpaEntity surplusCat = categoryRepository.findByCategoryCode("ADJ_SURPLUS")
                    .orElseThrow(() -> new IllegalStateException("Hệ thống chưa thiết lập danh mục ADJ_SURPLUS"));

            String memo = "Điều chỉnh thừa quỹ đối soát ngày " + reconciliation.getReconciliationDate()
                    + (command != null && command.reason() != null && !command.reason().isBlank() ? ": " + command.reason() : "");

            PostLedgerEntryCommand postCmd = new PostLedgerEntryCommand(
                    reconciliation.getAccountId(),
                    surplusCat.getId(),
                    EntryType.INFLOW,
                    variance,
                    reconciliation.getReconciliationDate(),
                    memo,
                    reconciliation.getEvidenceFileId(),
                    SourceType.MANUAL_ADJUSTMENT,
                    reconciliation.getId(),
                    "RECON_ADJUSTMENT"
            );
            postedEntry = cashLedgerService.postLedgerEntry(branchId, postCmd, actorId);
        } else {
            // Shortage -> OUTFLOW
            FinancialCategoryJpaEntity shortageCat = categoryRepository.findByCategoryCode("ADJ_SHORTAGE")
                    .orElseThrow(() -> new IllegalStateException("Hệ thống chưa thiết lập danh mục ADJ_SHORTAGE"));

            String memo = "Điều chỉnh thiếu quỹ đối soát ngày " + reconciliation.getReconciliationDate()
                    + (command != null && command.reason() != null && !command.reason().isBlank() ? ": " + command.reason() : "");

            PostLedgerEntryCommand postCmd = new PostLedgerEntryCommand(
                    reconciliation.getAccountId(),
                    shortageCat.getId(),
                    EntryType.OUTFLOW,
                    variance.abs(),
                    reconciliation.getReconciliationDate(),
                    memo,
                    reconciliation.getEvidenceFileId(),
                    SourceType.MANUAL_ADJUSTMENT,
                    reconciliation.getId(),
                    "RECON_ADJUSTMENT"
            );
            postedEntry = cashLedgerService.postLedgerEntry(branchId, postCmd, actorId);
        }

        reconciliation.setAdjustmentLedgerEntryId(postedEntry.id());
        reconciliation.setDisposition(ReconciliationDisposition.ADJUSTED);
        reconciliation.setConfirmedBy(actorId);
        reconciliation.setConfirmedAt(Instant.now());

        CashReconciliationJpaEntity saved = reconciliationRepository.save(reconciliation);
        FinancialAccountJpaEntity account = accountRepository.findById(saved.getAccountId()).orElse(null);
        Map<UUID, String> userNames = resolveUserNames();
        return toDto(saved, account, userNames);
    }

    /**
     * Closes a reconciliation record.
     */
    public CashReconciliationDto closeReconciliation(UUID branchId, UUID reconciliationId, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(reconciliationId, "Reconciliation ID must not be null");
        UUID actorId = currentUserId != null ? currentUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        CashReconciliationJpaEntity reconciliation = reconciliationRepository.findByBranchIdAndId(branchId, reconciliationId)
                .orElseThrow(() -> new ReconciliationNotFoundException(reconciliationId));

        reconciliation.setDisposition(ReconciliationDisposition.CLOSED);
        reconciliation.setConfirmedBy(actorId);
        reconciliation.setConfirmedAt(Instant.now());

        CashReconciliationJpaEntity saved = reconciliationRepository.save(reconciliation);
        FinancialAccountJpaEntity account = accountRepository.findById(saved.getAccountId()).orElse(null);
        Map<UUID, String> userNames = resolveUserNames();
        return toDto(saved, account, userNames);
    }

    /**
     * Lists reconciliations with optional filtering and pagination.
     */
    @Transactional(readOnly = true)
    public CashReconciliationPageResponse listReconciliations(
            UUID branchId,
            UUID accountId,
            LocalDate startDate,
            LocalDate endDate,
            ReconciliationDisposition disposition,
            Pageable pageable
    ) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Page<CashReconciliationJpaEntity> page = reconciliationRepository.findReconciliations(
                branchId, accountId, startDate, endDate, disposition, pageable
        );

        Set<UUID> accountIds = page.getContent().stream()
                .map(CashReconciliationJpaEntity::getAccountId)
                .collect(Collectors.toSet());

        Map<UUID, FinancialAccountJpaEntity> accounts = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, a -> a));

        Map<UUID, String> userNames = resolveUserNames();

        Page<CashReconciliationDto> dtoPage = page.map(e -> toDto(e, accounts.get(e.getAccountId()), userNames));
        return CashReconciliationPageResponse.of(dtoPage);
    }

    /**
     * Gets reconciliation detail by ID.
     */
    @Transactional(readOnly = true)
    public CashReconciliationDto getReconciliationDetail(UUID branchId, UUID reconciliationId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(reconciliationId, "Reconciliation ID must not be null");

        CashReconciliationJpaEntity reconciliation = reconciliationRepository.findByBranchIdAndId(branchId, reconciliationId)
                .orElseThrow(() -> new ReconciliationNotFoundException(reconciliationId));

        FinancialAccountJpaEntity account = accountRepository.findById(reconciliation.getAccountId()).orElse(null);
        Map<UUID, String> userNames = resolveUserNames();
        return toDto(reconciliation, account, userNames);
    }

    private CashReconciliationDto toDto(
            CashReconciliationJpaEntity entity,
            FinancialAccountJpaEntity account,
            Map<UUID, String> userNames
    ) {
        String accountName = account != null ? account.getAccountName() : "N/A";
        AccountKind accountKind = account != null ? account.getAccountKind() : AccountKind.CASH;
        String currency = account != null ? account.getCurrency() : "VND";
        String createdByName = entity.getCreatedBy() != null ? userNames.get(entity.getCreatedBy()) : null;
        String confirmedByName = entity.getConfirmedBy() != null ? userNames.get(entity.getConfirmedBy()) : null;
        String evidenceDownloadUrl = resolveEvidenceToken(entity.getEvidenceFileId(), entity.getBranchId());

        return new CashReconciliationDto(
                entity.getId(),
                entity.getBranchId(),
                entity.getAccountId(),
                accountName,
                accountKind,
                currency,
                entity.getReconciliationDate(),
                entity.getRecordedBalance(),
                entity.getObservedBalance(),
                entity.getVarianceAmount(),
                entity.getDisposition(),
                entity.getNote(),
                entity.getEvidenceFileId(),
                evidenceDownloadUrl,
                entity.getAdjustmentLedgerEntryId(),
                entity.getCreatedBy(),
                createdByName,
                entity.getConfirmedBy(),
                confirmedByName,
                entity.getConfirmedAt(),
                entity.getCreatedAt()
        );
    }

    private Map<UUID, String> resolveUserNames() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName, (v1, v2) -> v1));
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
