package com.mtms.hr.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.error.AccessForbiddenException;
import com.mtms.finance.application.CashLedgerService;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FinancialAccountNotFoundException;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.hr.application.dto.PayrollDtos.PayrollAdjustmentDto;
import com.mtms.hr.application.dto.PayrollPaymentDtos.BulkPaySalaryCommand;
import com.mtms.hr.application.dto.PayrollPaymentDtos.BulkPaySalaryResponse;
import com.mtms.hr.application.dto.PayrollPaymentDtos.BulkPaymentItemCommand;
import com.mtms.hr.application.dto.PayrollPaymentDtos.PaySalaryCommand;
import com.mtms.hr.application.dto.PayrollPaymentDtos.PayrollPaymentDto;
import com.mtms.hr.application.dto.PayrollPaymentDtos.PayslipDto;
import com.mtms.hr.domain.HrExceptions;
import com.mtms.hr.domain.PaymentMethod;
import com.mtms.hr.domain.PayrollLineNotFoundException;
import com.mtms.hr.domain.PayrollPaymentStatus;
import com.mtms.hr.domain.PayrollPeriodNotFoundException;
import com.mtms.hr.domain.PayrollPeriodStatus;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollAdjustmentJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollAdjustmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollLineJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollLineJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollPaymentJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollPaymentJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-HR-07 & P0-HR-08 Application Service.
 * Implements payroll salary payment recording linked to immutable cash ledger (TC-016),
 * privacy-guarded employee payslip lookups (TC-017), and primary-branch bulk settlements.
 */
@Service
@Transactional
public class PayrollPaymentService {

    private final PayrollLineJpaRepository lineRepository;
    private final PayrollPeriodJpaRepository periodRepository;
    private final PayrollAdjustmentJpaRepository adjustmentRepository;
    private final PayrollPaymentJpaRepository paymentRepository;
    private final EmployeeJpaRepository employeeRepository;
    private final FinancialAccountJpaRepository accountRepository;
    private final FinancialCategoryJpaRepository categoryRepository;
    private final CashLedgerService cashLedgerService;
    private final ObjectMapper objectMapper;

    public PayrollPaymentService(
            PayrollLineJpaRepository lineRepository,
            PayrollPeriodJpaRepository periodRepository,
            PayrollAdjustmentJpaRepository adjustmentRepository,
            PayrollPaymentJpaRepository paymentRepository,
            EmployeeJpaRepository employeeRepository,
            FinancialAccountJpaRepository accountRepository,
            FinancialCategoryJpaRepository categoryRepository,
            CashLedgerService cashLedgerService,
            ObjectMapper objectMapper
    ) {
        this.lineRepository = lineRepository;
        this.periodRepository = periodRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.paymentRepository = paymentRepository;
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.cashLedgerService = cashLedgerService;
        this.objectMapper = objectMapper;
    }

    /**
     * Pays salary for a single payroll line with row-level pessimistic locking.
     * Enforces invariants:
     * - Payroll period must be in APPROVED status.
     * - Payment amount > 0 and amount <= remaining debt (grossSalary - paidAmount).
     * - Posts OUTFLOW cash ledger entry under system category PAY_SALARY on paidOn date (TC-016).
     */
    public PayrollPaymentDto paySalary(UUID branchId, UUID lineId, PaySalaryCommand command, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(lineId, "Payroll Line ID must not be null");
        Objects.requireNonNull(command, "PaySalaryCommand must not be null");

        // 1. Lock line row pessimistically
        PayrollLineJpaEntity line = lineRepository.findByIdForUpdate(lineId)
                .orElseThrow(() -> new PayrollLineNotFoundException("Không tìm thấy dòng bảng lương: " + lineId));

        if (!branchId.equals(line.getExpenseBranchId())) {
            throw new AccessForbiddenException("Dòng lương không thuộc chi nhánh " + branchId);
        }

        // 2. Validate period and APPROVED status
        PayrollPeriodJpaEntity period = periodRepository.findById(line.getPayrollPeriodId())
                .orElseThrow(() -> new PayrollPeriodNotFoundException("Không tìm thấy kỳ bảng lương: " + line.getPayrollPeriodId()));

        if (!branchId.equals(period.getBranchId())) {
            throw new AccessForbiddenException("Kỳ bảng lương không thuộc chi nhánh " + branchId);
        }

        if (period.getStatus() != PayrollPeriodStatus.APPROVED) {
            throw new HrExceptions.PayrollNotApprovedException(
                    "Bảng lương kỳ " + period.getMonthStart() + " chưa được phê duyệt (APPROVED), không thể thực hiện chi trả.");
        }

        // 3. Validate payment amount against remaining unpaid gross salary
        BigDecimal amount = command.amount();
        BigDecimal currentPaid = line.getPaidAmount() != null ? line.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = line.getGrossSalary().subtract(currentPaid);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(remaining) > 0) {
            throw new HrExceptions.PayrollPaymentExceedsRemainingException(
                    "Số tiền chi trả (" + amount + ") vượt quá mức lương còn nợ (" + remaining + ").");
        }

        // 4. Validate financial account
        FinancialAccountJpaEntity account = accountRepository.findById(command.accountId())
                .filter(a -> branchId.equals(a.getBranchId()))
                .orElseThrow(() -> new FinancialAccountNotFoundException(
                        "Không tìm thấy tài khoản tài chính " + command.accountId() + " thuộc chi nhánh " + branchId));

        // 5. Lookup system category PAY_SALARY
        FinancialCategoryJpaEntity paySalaryCategory = categoryRepository.findByCategoryCode("PAY_SALARY")
                .orElseThrow(() -> new IllegalStateException("Danh mục hệ thống PAY_SALARY không tồn tại"));

        // 6. Post cash ledger entry (TC-016: date of cash outflow is command.paidOn)
        UUID paymentId = UUID.randomUUID();
        String memo = (command.note() != null && !command.note().isBlank())
                ? command.note()
                : ("Chi trả lương nhân viên - dòng " + line.getId());

        PostLedgerEntryCommand ledgerCommand = new PostLedgerEntryCommand(
                account.getId(),
                paySalaryCategory.getId(),
                EntryType.OUTFLOW,
                amount,
                command.paidOn(),
                memo,
                command.evidenceFileId(),
                SourceType.PAYROLL_PAYMENT,
                paymentId,
                "PAYMENT"
        );

        CashLedgerEntryDto ledgerEntry = cashLedgerService.postLedgerEntry(branchId, ledgerCommand, currentUserId);

        // 7. Update payroll line paid amount
        BigDecimal newPaidAmount = currentPaid.add(amount);
        line.setPaidAmount(newPaidAmount);
        line.setUpdatedAt(Instant.now());
        lineRepository.save(line);

        // 8. Create and persist PayrollPayment record
        PaymentMethod method = command.paymentMethod() != null ? command.paymentMethod() : PaymentMethod.BANK_TRANSFER;
        PayrollPaymentJpaEntity payment = new PayrollPaymentJpaEntity(
                paymentId,
                line.getId(),
                account.getId(),
                amount,
                command.paidOn(),
                method,
                PayrollPaymentStatus.POSTED,
                command.externalReference(),
                command.evidenceFileId(),
                ledgerEntry.id(),
                command.note(),
                currentUserId,
                Instant.now()
        );
        payrollPaymentRepositorySave(payment);

        return new PayrollPaymentDto(
                payment.getId(),
                payment.getPayrollLineId(),
                payment.getAccountId(),
                account.getAccountName(),
                payment.getAmount(),
                payment.getPaidOn(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getExternalReference(),
                payment.getLedgerEntryId(),
                payment.getNote(),
                newPaidAmount,
                line.getGrossSalary().subtract(newPaidAmount),
                payment.getCreatedAt()
        );
    }

    private void payrollPaymentRepositorySave(PayrollPaymentJpaEntity entity) {
        paymentRepository.save(entity);
    }

    /**
     * Executes bulk salary payments for a given approved payroll period.
     */
    public BulkPaySalaryResponse bulkPaySalary(UUID branchId, UUID periodId, BulkPaySalaryCommand command, UUID currentUserId) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(periodId, "Period ID must not be null");
        Objects.requireNonNull(command, "BulkPaySalaryCommand must not be null");

        PayrollPeriodJpaEntity period = periodRepository.findByIdAndBranchId(periodId, branchId)
                .orElseThrow(() -> new PayrollPeriodNotFoundException("Không tìm thấy kỳ bảng lương " + periodId));

        if (period.getStatus() != PayrollPeriodStatus.APPROVED) {
            throw new HrExceptions.PayrollNotApprovedException(
                    "Bảng lương kỳ " + period.getMonthStart() + " chưa được phê duyệt (APPROVED), không thể thực hiện chi trả.");
        }

        BigDecimal totalPaid = BigDecimal.ZERO;
        List<UUID> paymentIds = new ArrayList<>();

        for (BulkPaymentItemCommand item : command.items()) {
            PaySalaryCommand singleCommand = new PaySalaryCommand(
                    command.accountId(),
                    item.amount(),
                    command.paidOn(),
                    command.paymentMethod(),
                    item.externalReference(),
                    null,
                    command.note()
            );
            PayrollPaymentDto paid = paySalary(branchId, item.payrollLineId(), singleCommand, currentUserId);
            totalPaid = totalPaid.add(paid.amount());
            paymentIds.add(paid.id());
        }

        return new BulkPaySalaryResponse(totalPaid, paymentIds.size(), paymentIds);
    }

    /**
     * Retrieves all payment transactions for a given payroll line.
     */
    @Transactional(readOnly = true)
    public List<PayrollPaymentDto> getPaymentsForLine(UUID branchId, UUID lineId) {
        PayrollLineJpaEntity line = lineRepository.findById(lineId)
                .orElseThrow(() -> new PayrollLineNotFoundException("Không tìm thấy dòng bảng lương: " + lineId));

        if (!branchId.equals(line.getExpenseBranchId())) {
            throw new AccessForbiddenException("Dòng lương không thuộc chi nhánh " + branchId);
        }

        List<PayrollPaymentJpaEntity> payments = paymentRepository.findByPayrollLineIdOrderByPaidOnAscCreatedAtAsc(lineId);
        if (payments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> accountIds = payments.stream().map(PayrollPaymentJpaEntity::getAccountId).collect(Collectors.toSet());
        Map<UUID, String> accountNames = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(FinancialAccountJpaEntity::getId, FinancialAccountJpaEntity::getAccountName));

        return payments.stream().map(p -> new PayrollPaymentDto(
                p.getId(),
                p.getPayrollLineId(),
                p.getAccountId(),
                accountNames.getOrDefault(p.getAccountId(), ""),
                p.getAmount(),
                p.getPaidOn(),
                p.getPaymentMethod(),
                p.getStatus(),
                p.getExternalReference(),
                p.getLedgerEntryId(),
                p.getNote(),
                null,
                null,
                p.getCreatedAt()
        )).collect(Collectors.toList());
    }

    /**
     * Retrieves full payslip details for a payroll line.
     */
    @Transactional(readOnly = true)
    public PayslipDto getPayslip(UUID branchId, UUID lineId) {
        PayrollLineJpaEntity line = lineRepository.findById(lineId)
                .orElseThrow(() -> new PayrollLineNotFoundException("Không tìm thấy dòng bảng lương: " + lineId));

        if (!branchId.equals(line.getExpenseBranchId())) {
            throw new AccessForbiddenException("Dòng lương không thuộc chi nhánh " + branchId);
        }

        PayrollPeriodJpaEntity period = periodRepository.findById(line.getPayrollPeriodId())
                .orElseThrow(() -> new PayrollPeriodNotFoundException("Không tìm thấy kỳ bảng lương: " + line.getPayrollPeriodId()));

        EmployeeJpaEntity employee = employeeRepository.findById(line.getEmployeeId()).orElse(null);
        String staffCode = employee != null ? employee.getStaffCode() : "";
        String employeeName = employee != null ? employee.getFullName() : "";
        String positionName = employee != null ? employee.getPositionName() : "";

        List<PayrollAdjustmentJpaEntity> adjs = adjustmentRepository.findByPayrollLineIdOrderByCreatedAtAsc(lineId);
        List<PayrollAdjustmentDto> adjDtos = adjs.stream().map(adj -> new PayrollAdjustmentDto(
                adj.getId(),
                adj.getPayrollLineId(),
                adj.getAdjustmentCode().name(),
                adj.getSignedAmount(),
                adj.getExplanation(),
                adj.getApprovedBy(),
                adj.getCreatedAt()
        )).collect(Collectors.toList());

        List<PayrollPaymentDto> payments = getPaymentsForLine(branchId, lineId);

        BigDecimal paidAmount = line.getPaidAmount() != null ? line.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = line.getGrossSalary().subtract(paidAmount);

        return new PayslipDto(
                period.getId(),
                period.getBranchId(),
                period.getMonthStart(),
                period.getStatus().name(),
                line.getId(),
                line.getEmployeeId(),
                staffCode,
                employeeName,
                positionName,
                line.getRateType().name(),
                line.getApprovedMinutes(),
                line.getPayableWorkdays(),
                line.getBaseSalary(),
                line.getAdjustmentsTotal(),
                line.getGrossSalary(),
                paidAmount,
                remaining,
                parseJson(line.getPolicySnapshot()),
                parseJson(line.getCalculationSnapshot()),
                adjDtos,
                payments
        );
    }

    /**
     * Retrieves payslips for the current logged-in employee (TC-017 privacy guard).
     * Only displays APPROVED payroll periods.
     */
    @Transactional(readOnly = true)
    public List<PayslipDto> getMyPayslips(UUID branchId, UUID currentUserId, LocalDate monthStart) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(currentUserId, "User ID must not be null");

        EmployeeJpaEntity employee = employeeRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new AccessForbiddenException("Tài khoản chưa được liên kết với hồ sơ nhân viên"));

        List<PayrollLineJpaEntity> lines = lineRepository.findByEmployeeId(employee.getId());
        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        List<PayslipDto> results = new ArrayList<>();
        for (PayrollLineJpaEntity line : lines) {
            PayrollPeriodJpaEntity period = periodRepository.findById(line.getPayrollPeriodId()).orElse(null);
            if (period == null || !branchId.equals(period.getBranchId())) {
                continue;
            }

            // TC-017 / BR-HR-05: Employees can only view APPROVED payroll statements
            if (period.getStatus() != PayrollPeriodStatus.APPROVED) {
                continue;
            }

            if (monthStart != null && !period.getMonthStart().equals(monthStart)) {
                continue;
            }

            results.add(getPayslip(branchId, line.getId()));
        }

        results.sort(Comparator.comparing(PayslipDto::monthStart).reversed());
        return results;
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return Map.of();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
