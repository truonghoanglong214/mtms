package com.mtms.closing.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mtms.closing.domain.model.CloseComponentCode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ClosingDtos {

    private ClosingDtos() {}

    public record RevenueCheckDto(
            int totalDaysInMonth,
            int reportedDays,
            List<LocalDate> missingDates,
            boolean passed
    ) {}

    public record PayrollCheckDto(
            boolean periodExists,
            String status,
            BigDecimal totalGrossSalary,
            boolean passed
    ) {}

    public record ReconciliationCheckDto(
            int totalAccounts,
            int reconciledCount,
            boolean hasOpenDiscrepancies,
            boolean passed
    ) {}

    public record InventoryCheckDto(
            int consumptionIssuesCount,
            boolean passed
    ) {}

    public record ClosingPrerequisitesDto(
            UUID branchId,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate monthStart,
            boolean canClose,
            RevenueCheckDto revenueCheck,
            PayrollCheckDto payrollCheck,
            ReconciliationCheckDto reconciliationCheck,
            InventoryCheckDto inventoryCheck,
            String status,
            String blockReason
    ) {}

    public record NetRevenueDto(
            BigDecimal total,
            BigDecimal cashRevenue,
            BigDecimal bankRevenue,
            BigDecimal appNetRevenue
    ) {}

    public record CostOfGoodsSoldDto(
            BigDecimal total,
            boolean verifiedNoDoubleCost
    ) {}

    public record OperatingExpensesDto(
            BigDecimal payrollAccrual,
            BigDecimal otherOpex,
            BigDecimal totalOpex
    ) {}

    public record TaxObligationsDto(
            BigDecimal total
    ) {}

    public record CashFlowComparisonDto(
            BigDecimal cashInflow,
            BigDecimal cashOutflow,
            BigDecimal netCashFlow,
            BigDecimal accrualCashVariance,
            String varianceExplanation
    ) {}

    public record ManagerialPnLReportDto(
            UUID branchId,
            String branchName,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate monthStart,
            boolean isProvisional,
            String depreciationDisclaimer,
            NetRevenueDto netRevenue,
            CostOfGoodsSoldDto costOfGoodsSold,
            BigDecimal grossProfit,
            OperatingExpensesDto operatingExpenses,
            TaxObligationsDto taxObligations,
            BigDecimal managerialOperatingProfit,
            CashFlowComparisonDto cashFlowComparison
    ) {}

    public record ClosePeriodCommand(
            @NotNull(message = "monthStart is required")
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate monthStart,
            String note
    ) {}

    public record CloseComponentDto(
            UUID id,
            CloseComponentCode componentCode,
            BigDecimal signedAmount,
            String provenance
    ) {}

    public record MonthlyCloseDto(
            UUID id,
            UUID branchId,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate monthStart,
            int revision,
            String status,
            BigDecimal managerialProfit,
            boolean isProvisional,
            UUID closedBy,
            Instant closedAt,
            String snapshot,
            String note,
            List<CloseComponentDto> components
    ) {}

    public record CloseStatusDto(
            UUID branchId,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate monthStart,
            String status,
            boolean isClosed,
            Instant closedAt
    ) {}
}
