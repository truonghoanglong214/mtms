package com.mtms.closing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.audit.application.AuditLogService;
import com.mtms.audit.application.dto.RecordAuditEventCommand;
import com.mtms.closing.application.dto.ClosingDtos.*;
import com.mtms.closing.domain.ClosingExceptions.*;
import com.mtms.closing.domain.model.CloseComponent;
import com.mtms.closing.domain.model.CloseComponentCode;
import com.mtms.closing.domain.model.CloseStatus;
import com.mtms.closing.domain.model.MonthlyClose;
import com.mtms.closing.domain.port.MonthlyCloseRepository;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.infrastructure.persistence.*;
import com.mtms.hr.domain.PayrollPeriodStatus;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaRepository;
import com.mtms.inventory.domain.StockMovementType;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.revenue.domain.OperationStatus;
import com.mtms.revenue.domain.RevenueReportStatus;
import com.mtms.revenue.infrastructure.persistence.BusinessDayJpaEntity;
import com.mtms.revenue.infrastructure.persistence.BusinessDayJpaRepository;
import com.mtms.revenue.infrastructure.persistence.DailyRevenueReportJpaEntity;
import com.mtms.revenue.infrastructure.persistence.DailyRevenueReportJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ClosingService {

    public static final String DEPRECIATION_DISCLAIMER =
            "P0 P&L excludes depreciation (TC-051 / ADR-023). Chi phí khấu hao chưa được trích vào P&L tạm tính giai đoạn MVP.";

    private final MonthlyCloseRepository monthlyCloseRepository;
    private final DailyRevenueReportJpaRepository dailyRevenueReportRepository;
    private final BusinessDayJpaRepository businessDayRepository;
    private final PayrollPeriodJpaRepository payrollPeriodRepository;
    private final StockMovementJpaRepository stockMovementRepository;
    private final ExpenseJpaRepository expenseRepository;
    private final EntityTaxObligationJpaRepository taxObligationRepository;
    private final CashLedgerEntryJpaRepository cashLedgerEntryRepository;
    private final FinancialAccountJpaRepository financialAccountRepository;
    private final CashReconciliationJpaRepository cashReconciliationRepository;
    private final BranchJpaRepository branchRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public ClosingService(
            MonthlyCloseRepository monthlyCloseRepository,
            DailyRevenueReportJpaRepository dailyRevenueReportRepository,
            BusinessDayJpaRepository businessDayRepository,
            PayrollPeriodJpaRepository payrollPeriodRepository,
            StockMovementJpaRepository stockMovementRepository,
            ExpenseJpaRepository expenseRepository,
            EntityTaxObligationJpaRepository taxObligationRepository,
            CashLedgerEntryJpaRepository cashLedgerEntryRepository,
            FinancialAccountJpaRepository financialAccountRepository,
            CashReconciliationJpaRepository cashReconciliationRepository,
            BranchJpaRepository branchRepository,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.monthlyCloseRepository = monthlyCloseRepository;
        this.dailyRevenueReportRepository = dailyRevenueReportRepository;
        this.businessDayRepository = businessDayRepository;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.expenseRepository = expenseRepository;
        this.taxObligationRepository = taxObligationRepository;
        this.cashLedgerEntryRepository = cashLedgerEntryRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.cashReconciliationRepository = cashReconciliationRepository;
        this.branchRepository = branchRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public ClosingPrerequisitesDto checkPrerequisites(UUID branchId, LocalDate monthStart) {
        validateMonthStart(monthStart);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        int daysInMonth = monthStart.lengthOfMonth();

        // 1. Revenue reports check (BR-CLS-01, TC-040)
        List<DailyRevenueReportJpaEntity> confirmedReports = dailyRevenueReportRepository.findCurrentReports(
                branchId, monthStart, monthEnd, RevenueReportStatus.CONFIRMED, PageRequest.of(0, 100)
        ).getContent();
        Set<LocalDate> confirmedDates = confirmedReports.stream()
                .map(DailyRevenueReportJpaEntity::getBusinessDate)
                .collect(Collectors.toSet());

        List<BusinessDayJpaEntity> businessDays = businessDayRepository
                .findByBranchIdAndBusinessDateBetweenOrderByBusinessDateAsc(branchId, monthStart, monthEnd);
        Set<LocalDate> legallyClosedDates = businessDays.stream()
                .filter(b -> b.getOperationStatus() == OperationStatus.CLOSED)
                .map(BusinessDayJpaEntity::getBusinessDate)
                .collect(Collectors.toSet());

        List<LocalDate> missingDates = new ArrayList<>();
        LocalDate curr = monthStart;
        while (!curr.isAfter(monthEnd)) {
            if (!confirmedDates.contains(curr) && !legallyClosedDates.contains(curr)) {
                missingDates.add(curr);
            }
            curr = curr.plusDays(1);
        }

        int reportedDays = daysInMonth - missingDates.size();
        boolean revenuePassed = missingDates.isEmpty();
        RevenueCheckDto revenueCheck = new RevenueCheckDto(daysInMonth, reportedDays, missingDates, revenuePassed);

        // 2. Payroll period check (BR-CLS-01, TC-017, TC-040)
        Optional<PayrollPeriodJpaEntity> payrollOpt = payrollPeriodRepository
                .findByBranchIdAndMonthStartAndIsCurrentTrue(branchId, monthStart);
        boolean periodExists = payrollOpt.isPresent();
        String payrollStatus = payrollOpt.map(p -> p.getStatus().name()).orElse("NOT_CREATED");
        BigDecimal totalGrossSalary = payrollOpt.map(PayrollPeriodJpaEntity::getTotalGrossSalary).orElse(BigDecimal.ZERO);
        boolean payrollPassed = payrollOpt.map(p -> p.getStatus() == PayrollPeriodStatus.APPROVED).orElse(false);
        PayrollCheckDto payrollCheck = new PayrollCheckDto(periodExists, payrollStatus, totalGrossSalary, payrollPassed);

        // 3. Bank/Cash reconciliation check (P0-FIN-06)
        List<FinancialAccountJpaEntity> accounts = financialAccountRepository.findByBranchIdOrderByCreatedAtAsc(branchId);
        int totalAccounts = accounts.size();
        int reconciledCount = 0;
        for (FinancialAccountJpaEntity acc : accounts) {
            if (cashReconciliationRepository.existsByAccountIdAndReconciliationDate(acc.getId(), monthEnd)) {
                reconciledCount++;
            }
        }
        ReconciliationCheckDto reconciliationCheck = new ReconciliationCheckDto(
                totalAccounts, reconciledCount, false, true
        );

        // 4. Inventory check (P0-INV-07)
        long issuesCount = stockMovementRepository.findByBranchIdWithFilters(
                branchId, StockMovementType.MANUAL_ISSUE, null, monthStart, monthEnd, null, PageRequest.of(0, 1)
        ).getTotalElements();
        InventoryCheckDto inventoryCheck = new InventoryCheckDto((int) issuesCount, true);

        // Period status and block reason
        Optional<MonthlyClose> closeOpt = monthlyCloseRepository.findCurrentByBranchIdAndMonthStart(branchId, monthStart);
        String status = closeOpt.map(c -> c.status().name()).orElse("OPEN");

        boolean canClose = revenuePassed && payrollPassed;
        String blockReason = null;
        if (!canClose) {
            List<String> reasons = new ArrayList<>();
            if (!revenuePassed) {
                reasons.add("Còn " + missingDates.size() + " ngày chưa nộp báo cáo doanh thu");
            }
            if (!payrollPassed) {
                reasons.add("Bảng lương chi nhánh chưa được phê duyệt (trạng thái: " + payrollStatus + ")");
            }
            blockReason = String.join("; ", reasons) + ".";
        }

        return new ClosingPrerequisitesDto(
                branchId,
                monthStart,
                canClose,
                revenueCheck,
                payrollCheck,
                reconciliationCheck,
                inventoryCheck,
                status,
                blockReason
        );
    }

    public ManagerialPnLReportDto calculateManagerialPnL(UUID branchId, LocalDate monthStart) {
        validateMonthStart(monthStart);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        String branchName = branchRepository.findById(branchId)
                .map(BranchJpaEntity::getName)
                .orElse("Chi nhánh");

        // 1. Net Revenue Breakdown
        List<DailyRevenueReportJpaEntity> confirmedReports = dailyRevenueReportRepository.findCurrentReports(
                branchId, monthStart, monthEnd, RevenueReportStatus.CONFIRMED, PageRequest.of(0, 100)
        ).getContent();

        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal bankRevenue = BigDecimal.ZERO;
        BigDecimal appNetRevenue = BigDecimal.ZERO;
        for (DailyRevenueReportJpaEntity r : confirmedReports) {
            cashRevenue = cashRevenue.add(r.getCashAmount());
            bankRevenue = bankRevenue.add(r.getBankTransferAmount());
            appNetRevenue = appNetRevenue.add(r.getDeliveryAppNetAmount());
        }
        BigDecimal totalNetRevenue = cashRevenue.add(bankRevenue).add(appNetRevenue);
        NetRevenueDto netRevenue = new NetRevenueDto(totalNetRevenue, cashRevenue, bankRevenue, appNetRevenue);

        // 2. COGS (Issued Moving WAC)
        BigDecimal cogsSigned = stockMovementRepository.sumSignedValueByBranchIdAndMovementTypeAndDateBetween(
                branchId, StockMovementType.MANUAL_ISSUE, monthStart, monthEnd
        );
        BigDecimal cogsTotal = cogsSigned != null ? cogsSigned.abs() : BigDecimal.ZERO;
        CostOfGoodsSoldDto cogsDto = new CostOfGoodsSoldDto(cogsTotal, true);

        // Gross Profit
        BigDecimal grossProfit = totalNetRevenue.subtract(cogsTotal);

        // 3. Operating Expenses (Payroll Accrual + OPEX)
        Optional<PayrollPeriodJpaEntity> payrollOpt = payrollPeriodRepository
                .findByBranchIdAndMonthStartAndIsCurrentTrue(branchId, monthStart);
        BigDecimal payrollAccrual = payrollOpt.map(PayrollPeriodJpaEntity::getTotalGrossSalary).orElse(BigDecimal.ZERO);

        var expensesPage = expenseRepository.findExpenses(
                branchId, null, null, monthStart, monthEnd, null, null, PageRequest.of(0, 1000)
        );
        BigDecimal otherOpex = expensesPage.getContent().stream()
                .map(ExpenseJpaEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOpex = payrollAccrual.add(otherOpex);
        OperatingExpensesDto operatingExpenses = new OperatingExpensesDto(payrollAccrual, otherOpex, totalOpex);

        // 4. Tax obligations
        var taxPage = taxObligationRepository.findByBranchOrEntity(
                branchId, null, null, null, monthStart, monthEnd, PageRequest.of(0, 500)
        );
        BigDecimal taxTotal = taxPage.getContent().stream()
                .map(EntityTaxObligationJpaEntity::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxObligationsDto taxObligations = new TaxObligationsDto(taxTotal);

        // 5. Managerial Operating Profit (Provisional)
        BigDecimal managerialOperatingProfit = grossProfit.subtract(totalOpex).subtract(taxTotal);

        // 6. Cash Flow Comparison
        var cashEntries = cashLedgerEntryRepository.findEntries(
                branchId, null, null, null, monthStart, monthEnd, PageRequest.of(0, 5000)
        );
        BigDecimal cashInflow = BigDecimal.ZERO;
        BigDecimal cashOutflow = BigDecimal.ZERO;
        for (CashLedgerEntryJpaEntity e : cashEntries.getContent()) {
            if (e.getEntryType() == EntryType.INFLOW) {
                cashInflow = cashInflow.add(e.getAmount());
            } else if (e.getEntryType() == EntryType.OUTFLOW) {
                cashOutflow = cashOutflow.add(e.getAmount());
            }
        }
        BigDecimal netCashFlow = cashInflow.subtract(cashOutflow);
        BigDecimal accrualCashVariance = managerialOperatingProfit.subtract(netCashFlow);

        String varianceExplanation = "Chênh lệch " + accrualCashVariance.toPlainString() +
                " đ giữa lợi nhuận dồn tích và dòng tiền thực tế do doanh thu chưa đối soát về tài khoản ngân hàng, công nợ chi phí và lương phát sinh chưa chi.";
        CashFlowComparisonDto cashFlowComparison = new CashFlowComparisonDto(
                cashInflow, cashOutflow, netCashFlow, accrualCashVariance, varianceExplanation
        );

        return new ManagerialPnLReportDto(
                branchId,
                branchName,
                monthStart,
                true,
                DEPRECIATION_DISCLAIMER,
                netRevenue,
                cogsDto,
                grossProfit,
                operatingExpenses,
                taxObligations,
                managerialOperatingProfit,
                cashFlowComparison
        );
    }

    @Transactional
    public MonthlyCloseDto closePeriod(UUID branchId, LocalDate monthStart, String note, UUID actorUserId) {
        validateMonthStart(monthStart);

        if (monthlyCloseRepository.existsClosedByBranchIdAndMonthStart(branchId, monthStart)) {
            throw new MonthAlreadyClosedException(branchId, monthStart);
        }

        ClosingPrerequisitesDto prereqs = checkPrerequisites(branchId, monthStart);
        if (!prereqs.canClose()) {
            throw new ClosingPrerequisiteFailedException(
                    prereqs.blockReason() != null ? prereqs.blockReason() : "Điều kiện chốt sổ chưa đạt."
            );
        }

        ManagerialPnLReportDto pnl = calculateManagerialPnL(branchId, monthStart);

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(pnl);
        } catch (JsonProcessingException e) {
            snapshotJson = "{}";
        }

        UUID closeId = UUID.randomUUID();
        List<CloseComponent> components = List.of(
                CloseComponent.create(closeId, CloseComponentCode.REVENUE_TOTAL, pnl.netRevenue().total(), "{\"source\":\"daily_revenues\"}"),
                CloseComponent.create(closeId, CloseComponentCode.REVENUE_CASH, pnl.netRevenue().cashRevenue(), "{\"source\":\"daily_revenues_cash\"}"),
                CloseComponent.create(closeId, CloseComponentCode.REVENUE_BANK, pnl.netRevenue().bankRevenue(), "{\"source\":\"daily_revenues_bank\"}"),
                CloseComponent.create(closeId, CloseComponentCode.REVENUE_APP_NET, pnl.netRevenue().appNetRevenue(), "{\"source\":\"daily_revenues_app\"}"),
                CloseComponent.create(closeId, CloseComponentCode.COGS_TOTAL, pnl.costOfGoodsSold().total(), "{\"source\":\"stock_movements_cogs\"}"),
                CloseComponent.create(closeId, CloseComponentCode.GROSS_PROFIT, pnl.grossProfit(), "{\"source\":\"pnl_gross_profit\"}"),
                CloseComponent.create(closeId, CloseComponentCode.PAYROLL_ACCRUAL, pnl.operatingExpenses().payrollAccrual(), "{\"source\":\"payroll_periods\"}"),
                CloseComponent.create(closeId, CloseComponentCode.OPEX_TOTAL, pnl.operatingExpenses().totalOpex(), "{\"source\":\"expenses\"}"),
                CloseComponent.create(closeId, CloseComponentCode.TAX_OBLIGATIONS, pnl.taxObligations().total(), "{\"source\":\"entity_tax_obligations\"}"),
                CloseComponent.create(closeId, CloseComponentCode.MANAGERIAL_NET_PROFIT, pnl.managerialOperatingProfit(), "{\"source\":\"pnl_managerial_net_profit\"}"),
                CloseComponent.create(closeId, CloseComponentCode.CASH_NET_FLOW, pnl.cashFlowComparison().netCashFlow(), "{\"source\":\"cash_ledger_entries\"}"),
                CloseComponent.create(closeId, CloseComponentCode.ACCRUAL_CASH_VARIANCE, pnl.cashFlowComparison().accrualCashVariance(), "{\"source\":\"cash_variance\"}")
        );

        MonthlyClose monthlyClose = MonthlyClose.createClosed(
                branchId,
                monthStart,
                pnl.managerialOperatingProfit(),
                actorUserId,
                snapshotJson,
                note,
                components
        );

        MonthlyClose saved = monthlyCloseRepository.save(monthlyClose);

        // Audit log
        auditLogService.recordEvent(new RecordAuditEventCommand(
                branchId,
                actorUserId,
                null,
                "MONTHLY_CLOSE",
                saved.id(),
                "MONTH_CLOSED",
                note != null ? note : "Chốt sổ kỳ kinh doanh tháng " + monthStart,
                null,
                snapshotJson
        ));

        return toDto(saved);
    }

    public CloseStatusDto getCloseStatus(UUID branchId, LocalDate monthStart) {
        validateMonthStart(monthStart);
        Optional<MonthlyClose> closeOpt = monthlyCloseRepository.findCurrentByBranchIdAndMonthStart(branchId, monthStart);
        if (closeOpt.isPresent()) {
            MonthlyClose c = closeOpt.get();
            return new CloseStatusDto(branchId, monthStart, c.status().name(), c.isClosed(), c.closedAt());
        }
        return new CloseStatusDto(branchId, monthStart, "OPEN", false, null);
    }

    public List<MonthlyCloseDto> getCloseHistory(UUID branchId) {
        return monthlyCloseRepository.findByBranchIdOrderByMonthStartDesc(branchId).stream()
                .map(this::toDto)
                .toList();
    }

    public boolean isMonthClosed(UUID branchId, LocalDate date) {
        if (date == null) return false;
        LocalDate monthStart = date.withDayOfMonth(1);
        return monthlyCloseRepository.existsClosedByBranchIdAndMonthStart(branchId, monthStart);
    }

    private void validateMonthStart(LocalDate monthStart) {
        if (monthStart == null) {
            throw new InvalidMonthStartException("monthStart must not be null.");
        }
        if (monthStart.getDayOfMonth() != 1) {
            throw new InvalidMonthStartException("monthStart must be the first day of the month (YYYY-MM-01).");
        }
    }

    private MonthlyCloseDto toDto(MonthlyClose domain) {
        List<CloseComponentDto> compDtos = domain.components() != null
                ? domain.components().stream()
                .map(c -> new CloseComponentDto(
                        c.id(),
                        c.componentCode(),
                        c.signedAmount(),
                        c.provenance()
                ))
                .toList()
                : List.of();

        return new MonthlyCloseDto(
                domain.id(),
                domain.branchId(),
                domain.monthStart(),
                domain.revision(),
                domain.status().name(),
                domain.managerialProfit(),
                domain.isProvisional(),
                domain.closedBy(),
                domain.closedAt(),
                domain.snapshot(),
                domain.note(),
                compDtos
        );
    }
}
