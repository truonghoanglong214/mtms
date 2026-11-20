package com.mtms.finance.application;

import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.CreateTaxObligationCommand;
import com.mtms.finance.application.dto.PayTaxObligationCommand;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.application.dto.TaxObligationDetailDto;
import com.mtms.finance.application.dto.TaxObligationDto;
import com.mtms.finance.application.dto.TaxObligationPageResponse;
import com.mtms.finance.application.dto.TaxPaymentDto;
import com.mtms.finance.domain.CannotCancelPaidTaxException;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FinancialCategoryNotFoundException;
import com.mtms.finance.domain.PaymentMethod;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.domain.TaxObligationAlreadyExistsException;
import com.mtms.finance.domain.TaxObligationAlreadyPaidException;
import com.mtms.finance.domain.TaxObligationCancelledException;
import com.mtms.finance.domain.TaxObligationNotFoundException;
import com.mtms.finance.domain.TaxObligationStatus;
import com.mtms.finance.domain.TaxObligationType;
import com.mtms.finance.domain.TaxPaymentExceedsRemainingLiabilityException;
import com.mtms.finance.infrastructure.persistence.EntityTaxObligationJpaEntity;
import com.mtms.finance.infrastructure.persistence.EntityTaxObligationJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.TaxPaymentJpaEntity;
import com.mtms.finance.infrastructure.persistence.TaxPaymentJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
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
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * Application service managing manual tax obligations and payment installments.
 */
@Service
@Transactional
public class TaxObligationService {

    private final EntityTaxObligationJpaRepository obligationRepository;
    private final TaxPaymentJpaRepository paymentRepository;
    private final BranchJpaRepository branchRepository;
    private final FinancialAccountJpaRepository financialAccountRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final CashLedgerService cashLedgerService;

    public TaxObligationService(
            EntityTaxObligationJpaRepository obligationRepository,
            TaxPaymentJpaRepository paymentRepository,
            BranchJpaRepository branchRepository,
            FinancialAccountJpaRepository financialAccountRepository,
            FinancialCategoryJpaRepository categoryRepository,
            CashLedgerService cashLedgerService
    ) {
        this.obligationRepository = obligationRepository;
        this.paymentRepository = paymentRepository;
        this.branchRepository = branchRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.categoryRepository = categoryRepository;
        this.cashLedgerService = cashLedgerService;
    }

    /**
     * Creates a new tax manual obligation.
     * Enforces entity-level code uniqueness and period integrity constraints.
     */
    public TaxObligationDto createObligation(UUID branchId, CreateTaxObligationCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

        if (command.taxPeriodEnd().isBefore(command.taxPeriodStart())) {
            throw new IllegalArgumentException("Ngày kết thúc kỳ tính thuế không được trước ngày bắt đầu");
        }

        String obligationCode = command.obligationCode().trim();
        if (obligationRepository.existsByBusinessEntityIdAndObligationCode(branch.getBusinessEntityId(), obligationCode)) {
            throw new TaxObligationAlreadyExistsException(
                    "Mã nghĩa vụ thuế '" + obligationCode + "' đã tồn tại trong pháp nhân (TC-012)");
        }

        UUID assignedBranchId = Boolean.FALSE.equals(command.isBranchSpecific()) ? null : branchId;

        EntityTaxObligationJpaEntity entity = new EntityTaxObligationJpaEntity(
                UUID.randomUUID(),
                branch.getBusinessEntityId(),
                assignedBranchId,
                obligationCode,
                command.obligationType(),
                command.taxAuthorityName() != null ? command.taxAuthorityName().trim() : null,
                command.taxPeriodStart(),
                command.taxPeriodEnd(),
                command.dueDate(),
                command.amountDue(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TaxObligationStatus.OPEN,
                command.note(),
                command.evidenceFileId(),
                actorId,
                Instant.now()
        );

        EntityTaxObligationJpaEntity saved = obligationRepository.save(entity);
        return toDto(saved);
    }

    /**
     * Records a tax payment against an obligation.
     * Locks obligation row pessimistically, verifies remaining liability,
     * updates amount paid & status, and posts OUTFLOW to cash ledger if financial account is specified.
     */
    public TaxObligationDetailDto payObligation(UUID branchId, UUID obligationId, PayTaxObligationCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(obligationId, "Obligation ID must not be null");
        Objects.requireNonNull(command, "Command must not be null");
        UUID actorId = actorUserId != null ? actorUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

        EntityTaxObligationJpaEntity obligation = obligationRepository.findByIdForUpdate(obligationId)
                .orElseThrow(() -> new TaxObligationNotFoundException("Không tìm thấy nghĩa vụ thuế " + obligationId));

        // Enforce branch and business entity isolation
        boolean belongsToBranch = obligation.getBranchId() != null && obligation.getBranchId().equals(branchId);
        boolean belongsToEntityWide = obligation.getBranchId() == null && obligation.getBusinessEntityId().equals(branch.getBusinessEntityId());
        if (!belongsToBranch && !belongsToEntityWide) {
            throw new TaxObligationNotFoundException("Nghĩa vụ thuế " + obligationId + " không thuộc phạm vi chi nhánh " + branchId);
        }

        // Validate lifecycle state
        if (obligation.getStatus() == TaxObligationStatus.CANCELLED) {
            throw new TaxObligationCancelledException("Không thể nộp thuế cho nghĩa vụ thuế đã bị hủy (TC-012)");
        }
        if (obligation.getStatus() == TaxObligationStatus.PAID) {
            throw new TaxObligationAlreadyPaidException("Nghĩa vụ thuế này đã được thanh toán toàn bộ trước đó (TC-012)");
        }

        // Check remaining liability invariant
        BigDecimal remainingLiability = obligation.getAmountDue().subtract(obligation.getAmountPaid());
        if (command.amount().compareTo(remainingLiability) > 0) {
            throw new TaxPaymentExceedsRemainingLiabilityException(String.format(
                    "Số tiền nộp %s vượt quá nghĩa vụ thuế còn lại %s (TC-012)",
                    command.amount(), remainingLiability));
        }

        UUID paymentId = UUID.randomUUID();
        UUID cashLedgerEntryId = null;

        // Post cash ledger entry if account is provided
        if (command.financialAccountId() != null) {
            FinancialCategoryJpaEntity taxCategory = categoryRepository.findByCategoryCode("TAX_PAYMENT")
                    .orElseThrow(() -> new FinancialCategoryNotFoundException("Không tìm thấy danh mục hệ thống TAX_PAYMENT"));

            String memo = (command.note() != null && !command.note().isBlank())
                    ? command.note()
                    : "Nộp thuế " + obligation.getObligationCode();

            PostLedgerEntryCommand ledgerCommand = new PostLedgerEntryCommand(
                    command.financialAccountId(),
                    taxCategory.getId(),
                    EntryType.OUTFLOW,
                    command.amount(),
                    command.paymentDate(),
                    memo,
                    command.evidenceFileId(),
                    SourceType.TAX_PAYMENT,
                    paymentId,
                    "TAX_PAYMENT"
            );

            CashLedgerEntryDto ledgerEntry = cashLedgerService.postLedgerEntry(branchId, ledgerCommand, actorId);
            cashLedgerEntryId = ledgerEntry.id();
        }

        // Save tax payment record
        TaxPaymentJpaEntity payment = new TaxPaymentJpaEntity(
                paymentId,
                obligation.getId(),
                branchId,
                command.paymentDate(),
                command.amount(),
                command.paymentMethod() != null ? command.paymentMethod() : PaymentMethod.BANK_TRANSFER,
                command.financialAccountId(),
                cashLedgerEntryId,
                command.receiptNumber(),
                command.note(),
                command.evidenceFileId(),
                actorId,
                Instant.now()
        );
        paymentRepository.save(payment);

        // Update obligation paid amount and status
        BigDecimal newAmountPaid = obligation.getAmountPaid().add(command.amount());
        obligation.setAmountPaid(newAmountPaid);
        if (newAmountPaid.compareTo(obligation.getAmountDue()) >= 0) {
            obligation.setStatus(TaxObligationStatus.PAID);
        } else {
            obligation.setStatus(TaxObligationStatus.PARTIALLY_PAID);
        }
        obligationRepository.save(obligation);

        return getObligationDetail(branchId, obligation.getId());
    }

    /**
     * Cancels an open tax obligation. Only allowed when amountPaid == 0.
     */
    public TaxObligationDto cancelObligation(UUID branchId, UUID obligationId, UUID actorUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(obligationId, "Obligation ID must not be null");

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

        EntityTaxObligationJpaEntity obligation = obligationRepository.findByIdForUpdate(obligationId)
                .orElseThrow(() -> new TaxObligationNotFoundException("Không tìm thấy nghĩa vụ thuế " + obligationId));

        boolean belongsToBranch = obligation.getBranchId() != null && obligation.getBranchId().equals(branchId);
        boolean belongsToEntityWide = obligation.getBranchId() == null && obligation.getBusinessEntityId().equals(branch.getBusinessEntityId());
        if (!belongsToBranch && !belongsToEntityWide) {
            throw new TaxObligationNotFoundException("Nghĩa vụ thuế " + obligationId + " không thuộc phạm vi chi nhánh " + branchId);
        }

        if (obligation.getStatus() == TaxObligationStatus.CANCELLED) {
            return toDto(obligation);
        }

        if (obligation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0
                || obligation.getStatus() == TaxObligationStatus.PAID
                || obligation.getStatus() == TaxObligationStatus.PARTIALLY_PAID) {
            throw new CannotCancelPaidTaxException("Không thể hủy nghĩa vụ thuế đã có phát sinh thanh toán (TC-012)");
        }

        obligation.setStatus(TaxObligationStatus.CANCELLED);
        EntityTaxObligationJpaEntity saved = obligationRepository.save(obligation);
        return toDto(saved);
    }

    /**
     * Retrieves paginated list of tax obligations visible to a branch.
     */
    @Transactional(readOnly = true)
    public TaxObligationPageResponse listObligations(
            UUID branchId,
            TaxObligationType obligationType,
            TaxObligationStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

        Page<TaxObligationDto> page = obligationRepository.findByBranchOrEntity(
                branchId,
                branch.getBusinessEntityId(),
                status,
                obligationType,
                startDate,
                endDate,
                pageable
        ).map(this::toDto);

        return TaxObligationPageResponse.of(page);
    }

    /**
     * Retrieves detail of a single tax obligation including payment history.
     */
    @Transactional(readOnly = true)
    public TaxObligationDetailDto getObligationDetail(UUID branchId, UUID obligationId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(obligationId, "Obligation ID must not be null");

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh: " + branchId));

        EntityTaxObligationJpaEntity obligation = obligationRepository.findById(obligationId)
                .orElseThrow(() -> new TaxObligationNotFoundException("Không tìm thấy nghĩa vụ thuế " + obligationId));

        boolean belongsToBranch = obligation.getBranchId() != null && obligation.getBranchId().equals(branchId);
        boolean belongsToEntityWide = obligation.getBranchId() == null && obligation.getBusinessEntityId().equals(branch.getBusinessEntityId());
        if (!belongsToBranch && !belongsToEntityWide) {
            throw new TaxObligationNotFoundException("Nghĩa vụ thuế " + obligationId + " không thuộc phạm vi chi nhánh " + branchId);
        }

        List<TaxPaymentJpaEntity> paymentEntities = paymentRepository.findByObligationIdOrderByPaymentDateAscCreatedAtAsc(obligationId);

        Map<UUID, String> accountNames = financialAccountRepository.findByBranchIdOrderByCreatedAtAsc(branchId)
                .stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, FinancialAccountJpaEntity::getAccountName));

        List<TaxPaymentDto> paymentDtos = paymentEntities.stream()
                .map(p -> new TaxPaymentDto(
                        p.getId(),
                        p.getObligationId(),
                        p.getBranchId(),
                        p.getPaymentDate(),
                        p.getAmount(),
                        p.getPaymentMethod(),
                        p.getFinancialAccountId(),
                        p.getFinancialAccountId() != null ? accountNames.get(p.getFinancialAccountId()) : null,
                        p.getCashLedgerEntryId(),
                        p.getReceiptNumber(),
                        p.getNote(),
                        p.getEvidenceFileId(),
                        p.getCreatedBy(),
                        p.getCreatedAt()
                ))
                .toList();

        BigDecimal remainingLiability = obligation.getAmountDue().subtract(obligation.getAmountPaid());

        return new TaxObligationDetailDto(
                obligation.getId(),
                obligation.getBusinessEntityId(),
                obligation.getBranchId(),
                obligation.getObligationCode(),
                obligation.getObligationType(),
                obligation.getTaxAuthorityName(),
                obligation.getTaxPeriodStart(),
                obligation.getTaxPeriodEnd(),
                obligation.getDueDate(),
                obligation.getAmountDue(),
                obligation.getAmountPaid(),
                obligation.getAmountAllocated(),
                remainingLiability,
                obligation.getStatus(),
                obligation.getNote(),
                obligation.getEvidenceFileId(),
                obligation.getEnteredBy(),
                obligation.getCreatedAt(),
                paymentDtos
        );
    }

    private TaxObligationDto toDto(EntityTaxObligationJpaEntity entity) {
        BigDecimal remainingLiability = entity.getAmountDue().subtract(entity.getAmountPaid());
        return new TaxObligationDto(
                entity.getId(),
                entity.getBusinessEntityId(),
                entity.getBranchId(),
                entity.getObligationCode(),
                entity.getObligationType(),
                entity.getTaxAuthorityName(),
                entity.getTaxPeriodStart(),
                entity.getTaxPeriodEnd(),
                entity.getDueDate(),
                entity.getAmountDue(),
                entity.getAmountPaid(),
                entity.getAmountAllocated(),
                remainingLiability,
                entity.getStatus(),
                entity.getNote(),
                entity.getEvidenceFileId(),
                entity.getEnteredBy(),
                entity.getCreatedAt()
        );
    }
}
