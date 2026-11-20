package com.mtms.finance.application;

import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.CreateReceivableCommand;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.application.dto.ReceivableDetailDto;
import com.mtms.finance.application.dto.ReceivableDto;
import com.mtms.finance.application.dto.ReceivableReceiptDto;
import com.mtms.finance.application.dto.RecordReceiptCommand;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.ReceivableNotFoundException;
import com.mtms.finance.domain.ReceivableStatus;
import com.mtms.finance.domain.ReceiptExceedsRemainingReceivableException;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.ReceivableJpaEntity;
import com.mtms.finance.infrastructure.persistence.ReceivableJpaRepository;
import com.mtms.finance.infrastructure.persistence.ReceivableReceiptJpaEntity;
import com.mtms.finance.infrastructure.persistence.ReceivableReceiptJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-FIN-05
 * Application service managing receivables ledger and money collections.
 */
@Service
@Transactional
public class ReceivableService {

    private final ReceivableJpaRepository receivableRepository;
    private final ReceivableReceiptJpaRepository receiptRepository;
    private final FinancialAccountJpaRepository financialAccountRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final CashLedgerService cashLedgerService;
    private final BranchJpaRepository branchRepository;

    public ReceivableService(
            ReceivableJpaRepository receivableRepository,
            ReceivableReceiptJpaRepository receiptRepository,
            FinancialAccountJpaRepository financialAccountRepository,
            FinancialCategoryJpaRepository categoryRepository,
            CashLedgerService cashLedgerService,
            BranchJpaRepository branchRepository
    ) {
        this.receivableRepository = receivableRepository;
        this.receiptRepository = receiptRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.categoryRepository = categoryRepository;
        this.cashLedgerService = cashLedgerService;
        this.branchRepository = branchRepository;
    }

    /**
     * Retrieves paginated list of receivables for a branch.
     */
    @Transactional(readOnly = true)
    public Page<ReceivableDto> getReceivables(UUID branchId, ReceivableStatus status, String search, Pageable pageable) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        return receivableRepository.findByBranchIdWithFilters(branchId, status, search, pageable)
                .map(this::toReceivableDto);
    }

    /**
     * Retrieves detailed information of a receivable including receipt history.
     */
    @Transactional(readOnly = true)
    public ReceivableDetailDto getReceivableDetail(UUID branchId, UUID receivableId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(receivableId, "Receivable ID must not be null");

        ReceivableJpaEntity receivable = receivableRepository.findByBranchIdAndId(branchId, receivableId)
                .orElseThrow(() -> new ReceivableNotFoundException("Không tìm thấy khoản phải thu " + receivableId + " thuộc chi nhánh " + branchId));

        List<ReceivableReceiptJpaEntity> receiptEntities = receiptRepository.findByReceivableIdOrderByReceivedOnAscCreatedAtAsc(receivableId);

        Map<UUID, String> accountNames = financialAccountRepository.findByBranchIdOrderByCreatedAtAsc(branchId)
                .stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, FinancialAccountJpaEntity::getAccountName, (a, b) -> a));

        List<ReceivableReceiptDto> receipts = receiptEntities.stream()
                .map(r -> new ReceivableReceiptDto(
                        r.getId(),
                        r.getReceivableId(),
                        r.getAccountId(),
                        accountNames.getOrDefault(r.getAccountId(), "Tài khoản không xác định"),
                        r.getAmount(),
                        r.getReceivedOn(),
                        r.getPaymentMethod(),
                        r.getBankReference(),
                        r.getEvidenceFileId(),
                        r.getStatus(),
                        r.getPostedBy(),
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ReceivableDetailDto.of(
                receivable.getId(),
                receivable.getBusinessEntityId(),
                receivable.getBranchId(),
                receivable.getCounterpartyLabel(),
                receivable.getSourceType(),
                receivable.getSourceId(),
                receivable.getIncurredDate(),
                receivable.getDueDate(),
                receivable.getOriginalAmount(),
                receivable.getReceivedAmount(),
                receivable.getStatus(),
                receivable.getNotes(),
                receivable.getCreatedBy(),
                receivable.getCreatedAt(),
                receipts
        );
    }

    /**
     * Creates a manual receivable record.
     */
    public ReceivableDto createReceivable(UUID branchId, CreateReceivableCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

        ReceivableJpaEntity entity = new ReceivableJpaEntity(
                UUID.randomUUID(),
                branch.getBusinessEntityId(),
                branch.getId(),
                command.counterpartyLabel().trim(),
                command.sourceType() != null ? command.sourceType() : "MANUAL_RECEIVABLE",
                command.sourceId(),
                command.incurredDate(),
                command.dueDate(),
                command.originalAmount(),
                BigDecimal.ZERO,
                ReceivableStatus.OPEN,
                command.notes(),
                actorId,
                Instant.now()
        );

        ReceivableJpaEntity saved = receivableRepository.save(entity);
        return toReceivableDto(saved);
    }

    /**
     * Records collection of funds against a receivable.
     */
    public ReceivableReceiptDto recordReceipt(UUID branchId, UUID receivableId, RecordReceiptCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(receivableId, "Receivable ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        ReceivableJpaEntity receivable = receivableRepository.findByBranchIdAndIdForUpdate(branchId, receivableId)
                .orElseThrow(() -> new ReceivableNotFoundException("Không tìm thấy khoản phải thu " + receivableId + " thuộc chi nhánh " + branchId));

        if (receivable.getStatus() == ReceivableStatus.SETTLED) {
            throw new IllegalArgumentException("Khoản phải thu này đã được thu hồi đầy đủ");
        }

        BigDecimal remaining = receivable.getOriginalAmount().subtract(receivable.getReceivedAmount());
        if (command.amount().compareTo(remaining) > 0) {
            throw new ReceiptExceedsRemainingReceivableException(String.format(
                    "Số tiền thu %s vượt quá số tiền còn lại phải thu %s",
                    command.amount(), remaining));
        }

        BigDecimal newReceivedAmount = receivable.getReceivedAmount().add(command.amount());
        receivable.setReceivedAmount(newReceivedAmount);
        if (newReceivedAmount.compareTo(receivable.getOriginalAmount()) == 0) {
            receivable.setStatus(ReceivableStatus.SETTLED);
        }
        receivable = receivableRepository.save(receivable);

        ReceivableReceiptJpaEntity receipt = new ReceivableReceiptJpaEntity(
                UUID.randomUUID(),
                receivable.getId(),
                command.accountId(),
                command.amount(),
                command.receivedOn(),
                command.paymentMethod(),
                command.bankReference(),
                command.evidenceFileId(),
                "POSTED",
                actorId,
                Instant.now()
        );
        receipt = receiptRepository.save(receipt);

        FinancialCategoryJpaEntity recCategory = categoryRepository.findByCategoryCode("REC_OTHER")
                .or(() -> categoryRepository.findByCategoryCode("REV_CASH"))
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy danh mục tài chính để ghi nhận thu tiền"));

        PostLedgerEntryCommand ledgerCommand = new PostLedgerEntryCommand(
                command.accountId(),
                recCategory.getId(),
                EntryType.INFLOW,
                command.amount(),
                command.receivedOn(),
                "Thu hồi công nợ phải thu: " + receivable.getCounterpartyLabel(),
                command.evidenceFileId(),
                SourceType.RECEIVABLE_RECEIPT,
                receipt.getId(),
                "MAIN"
        );
        CashLedgerEntryDto ledgerEntry = cashLedgerService.postLedgerEntry(branchId, ledgerCommand, actorId);

        FinancialAccountJpaEntity account = financialAccountRepository.findById(command.accountId()).orElse(null);
        String accountName = account != null ? account.getAccountName() : "Tài khoản";

        return new ReceivableReceiptDto(
                receipt.getId(),
                receipt.getReceivableId(),
                receipt.getAccountId(),
                accountName,
                receipt.getAmount(),
                receipt.getReceivedOn(),
                receipt.getPaymentMethod(),
                receipt.getBankReference(),
                receipt.getEvidenceFileId(),
                receipt.getStatus(),
                receipt.getPostedBy(),
                receipt.getCreatedAt()
        );
    }

    private ReceivableDto toReceivableDto(ReceivableJpaEntity entity) {
        return ReceivableDto.of(
                entity.getId(),
                entity.getBusinessEntityId(),
                entity.getBranchId(),
                entity.getCounterpartyLabel(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getIncurredDate(),
                entity.getDueDate(),
                entity.getOriginalAmount(),
                entity.getReceivedAmount(),
                entity.getStatus(),
                entity.getNotes(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }
}
