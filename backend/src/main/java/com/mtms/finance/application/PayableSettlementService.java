package com.mtms.finance.application;

import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.CreateManualPayableCommand;
import com.mtms.finance.application.dto.PayableDetailDto;
import com.mtms.finance.application.dto.PayableDto;
import com.mtms.finance.application.dto.PayablePaymentDto;
import com.mtms.finance.application.dto.PayablePaymentResponseDto;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.application.dto.SettlePayableCommand;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.PayableAlreadySettledException;
import com.mtms.finance.domain.PayableNotFoundException;
import com.mtms.finance.domain.PaymentExceedsRemainingDebtException;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.PayablePaymentJpaEntity;
import com.mtms.finance.infrastructure.persistence.PayablePaymentJpaRepository;
import com.mtms.inventory.domain.PayableStatus;
import com.mtms.inventory.domain.PaymentStatus;
import com.mtms.inventory.infrastructure.persistence.PayableJpaEntity;
import com.mtms.inventory.infrastructure.persistence.PayableJpaRepository;
import com.mtms.inventory.infrastructure.persistence.PurchaseReceiptJpaEntity;
import com.mtms.inventory.infrastructure.persistence.PurchaseReceiptJpaRepository;
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
 * P0-FIN-05 / BR-FIN-02 / TC-009
 * Application service managing branch payables ledger, manual debts, and debt settlement installments.
 */
@Service
@Transactional
public class PayableSettlementService {

    private final PayableJpaRepository payableRepository;
    private final PayablePaymentJpaRepository paymentRepository;
    private final PurchaseReceiptJpaRepository purchaseReceiptRepository;
    private final FinancialAccountJpaRepository financialAccountRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final CashLedgerService cashLedgerService;
    private final BranchJpaRepository branchRepository;

    public PayableSettlementService(
            PayableJpaRepository payableRepository,
            PayablePaymentJpaRepository paymentRepository,
            PurchaseReceiptJpaRepository purchaseReceiptRepository,
            FinancialAccountJpaRepository financialAccountRepository,
            FinancialCategoryJpaRepository categoryRepository,
            CashLedgerService cashLedgerService,
            BranchJpaRepository branchRepository
    ) {
        this.payableRepository = payableRepository;
        this.paymentRepository = paymentRepository;
        this.purchaseReceiptRepository = purchaseReceiptRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.categoryRepository = categoryRepository;
        this.cashLedgerService = cashLedgerService;
        this.branchRepository = branchRepository;
    }

    /**
     * Retrieves paginated list of payables for a branch.
     */
    @Transactional(readOnly = true)
    public Page<PayableDto> getPayables(UUID branchId, PayableStatus status, String search, Pageable pageable) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        return payableRepository.findByBranchIdWithFilters(branchId, status, search, pageable)
                .map(this::toPayableDto);
    }

    /**
     * Retrieves detailed information of a payable including its payment history.
     */
    @Transactional(readOnly = true)
    public PayableDetailDto getPayableDetail(UUID branchId, UUID payableId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(payableId, "Payable ID must not be null");

        PayableJpaEntity payable = payableRepository.findByBranchIdAndId(branchId, payableId)
                .orElseThrow(() -> new PayableNotFoundException("Không tìm thấy khoản nợ phải trả " + payableId + " thuộc chi nhánh " + branchId));

        List<PayablePaymentJpaEntity> paymentEntities = paymentRepository.findByPayableIdOrderByPaidOnAscCreatedAtAsc(payableId);

        Map<UUID, String> accountNames = financialAccountRepository.findByBranchIdOrderByCreatedAtAsc(branchId)
                .stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, FinancialAccountJpaEntity::getAccountName, (a, b) -> a));

        List<PayablePaymentDto> payments = paymentEntities.stream()
                .map(p -> new PayablePaymentDto(
                        p.getId(),
                        p.getPayableId(),
                        p.getAccountId(),
                        accountNames.getOrDefault(p.getAccountId(), "Tài khoản không xác định"),
                        p.getAmount(),
                        p.getPaidOn(),
                        p.getPaymentMethod(),
                        p.getBankReference(),
                        p.getEvidenceFileId(),
                        p.getStatus(),
                        p.getPostedBy(),
                        p.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return PayableDetailDto.of(
                payable.getId(),
                payable.getBusinessEntityId(),
                payable.getBranchId(),
                payable.getCounterpartyLabel(),
                payable.getSourceType(),
                payable.getSourceId(),
                payable.getIncurredDate(),
                payable.getDueDate(),
                payable.getOriginalAmount(),
                payable.getPaidAmount(),
                payable.getStatus(),
                payable.getCreatedAt(),
                payments
        );
    }

    /**
     * Creates a manual payable debt record (e.g. equipment installment, facility repairs).
     */
    public PayableDto createManualPayable(UUID branchId, CreateManualPayableCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

        PayableJpaEntity entity = new PayableJpaEntity(
                UUID.randomUUID(),
                branch.getBusinessEntityId(),
                branch.getId(),
                command.counterpartyLabel().trim(),
                "MANUAL_PAYABLE",
                null,
                command.incurredDate(),
                command.dueDate(),
                command.originalAmount(),
                BigDecimal.ZERO,
                PayableStatus.OPEN,
                Instant.now()
        );

        PayableJpaEntity saved = payableRepository.save(entity);
        return toPayableDto(saved);
    }

    /**
     * Settles partial or full debt against an outstanding payable.
     * Enforces TC-009: Locks row pessimistically, verifies amount <= remaining debt,
     * updates payable and linked purchase receipt, records payment history, and posts OUTFLOW to cash ledger.
     */
    public PayablePaymentResponseDto settlePayable(UUID branchId, UUID payableId, SettlePayableCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(payableId, "Payable ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        // 1. Lock payable row with pessimistic write lock
        PayableJpaEntity payable = payableRepository.findByBranchIdAndIdForUpdate(branchId, payableId)
                .orElseThrow(() -> new PayableNotFoundException("Không tìm thấy khoản nợ phải trả " + payableId + " thuộc chi nhánh " + branchId));

        // 2. Validate payable is not already settled
        if (payable.getStatus() == PayableStatus.SETTLED) {
            throw new PayableAlreadySettledException("Khoản nợ này đã được thanh toán toàn bộ trước đó");
        }

        // 3. Check remaining debt invariant: payment amount <= remaining debt
        BigDecimal remainingDebt = payable.getOriginalAmount().subtract(payable.getPaidAmount());
        if (command.amount().compareTo(remainingDebt) > 0) {
            throw new PaymentExceedsRemainingDebtException(String.format(
                    "Số tiền thanh toán %s vượt quá số nợ còn lại %s",
                    command.amount(), remainingDebt));
        }

        // 4. Update payable paid amount and status
        BigDecimal newPaidAmount = payable.getPaidAmount().add(command.amount());
        payable.setPaidAmount(newPaidAmount);
        if (newPaidAmount.compareTo(payable.getOriginalAmount()) == 0) {
            payable.setStatus(PayableStatus.SETTLED);
        }
        payable = payableRepository.save(payable);
        BigDecimal updatedRemainingDebt = payable.getOriginalAmount().subtract(newPaidAmount);

        // 5. If payable is linked to purchase receipt, update receipt's paymentStatus synchronously
        PaymentStatus receiptPaymentStatus = null;
        if ("PURCHASE_RECEIPT".equals(payable.getSourceType()) && payable.getSourceId() != null) {
            var optReceipt = purchaseReceiptRepository.findById(payable.getSourceId());
            if (optReceipt.isPresent()) {
                PurchaseReceiptJpaEntity receipt = optReceipt.get();
                receiptPaymentStatus = (payable.getStatus() == PayableStatus.SETTLED)
                        ? PaymentStatus.PAID
                        : PaymentStatus.PARTIALLY_PAID;
                receipt.setPaymentStatus(receiptPaymentStatus);
                purchaseReceiptRepository.save(receipt);
            }
        }

        // 6. Record payment installment in mtms.payable_payments
        PayablePaymentJpaEntity paymentEntity = new PayablePaymentJpaEntity(
                UUID.randomUUID(),
                payable.getId(),
                command.accountId(),
                command.amount(),
                command.paidOn(),
                command.paymentMethod(),
                command.bankReference(),
                command.evidenceFileId(),
                "POSTED",
                actorId,
                Instant.now()
        );
        paymentEntity = paymentRepository.save(paymentEntity);

        // 7. Post OUTFLOW entry into cash/bank subledger (TC-009: category DEBT_SUPPLIER, group DEBT_SETTLEMENT)
        FinancialCategoryJpaEntity debtCategory = categoryRepository.findByCategoryCode("DEBT_SUPPLIER")
                .orElseThrow(() -> new IllegalStateException("Danh mục hệ thống DEBT_SUPPLIER không tồn tại"));

        PostLedgerEntryCommand ledgerCommand = new PostLedgerEntryCommand(
                command.accountId(),
                debtCategory.getId(),
                EntryType.OUTFLOW,
                command.amount(),
                command.paidOn(),
                "Thanh toán công nợ NCC: " + payable.getCounterpartyLabel(),
                command.evidenceFileId(),
                SourceType.SUPPLIER_PAYMENT,
                paymentEntity.getId(),
                "MAIN"
        );
        CashLedgerEntryDto ledgerEntry = cashLedgerService.postLedgerEntry(branchId, ledgerCommand, actorId);

        return new PayablePaymentResponseDto(
                paymentEntity.getId(),
                payable.getId(),
                command.accountId(),
                command.amount(),
                command.paidOn(),
                command.paymentMethod(),
                command.bankReference(),
                command.evidenceFileId(),
                updatedRemainingDebt,
                payable.getStatus(),
                receiptPaymentStatus,
                ledgerEntry.id(),
                paymentEntity.getCreatedAt()
        );
    }

    private PayableDto toPayableDto(PayableJpaEntity entity) {
        return PayableDto.of(
                entity.getId(),
                entity.getBusinessEntityId(),
                entity.getBranchId(),
                entity.getCounterpartyLabel(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getIncurredDate(),
                entity.getDueDate(),
                entity.getOriginalAmount(),
                entity.getPaidAmount(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
