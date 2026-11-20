package com.mtms.revenue.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.common.idempotency.Idempotent;
import com.mtms.finance.application.RevenueCashLinkageService;
import com.mtms.finance.application.dto.CashLedgerEntryDto;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.revenue.application.dto.ConfirmRevenueCommand;
import com.mtms.revenue.application.dto.CreateDailyRevenueCommand;
import com.mtms.revenue.application.dto.DailyRevenueListResponse;
import com.mtms.revenue.application.dto.DailyRevenueReportDto;
import com.mtms.revenue.application.service.BusinessCalendarService;
import com.mtms.revenue.application.service.DailyRevenueService;
import com.mtms.revenue.domain.RevenueReportStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * P0-REV-02 / P0-REV-03 / BR-REV-01 / BR-REV-03 / TC-006 / P0-REV-05 / BR-REV-04
 * REST Controller for branch daily revenue reporting, submission, confirmation, queries,
 * and linked cash/bank ledger entries.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/daily-revenue")
public class DailyRevenueController {

    private final DailyRevenueService dailyRevenueService;
    private final BusinessCalendarService businessCalendarService;
    private final BranchAuthorizationService branchAuthorizationService;
    private final RevenueCashLinkageService revenueCashLinkageService;

    public DailyRevenueController(
            DailyRevenueService dailyRevenueService,
            BusinessCalendarService businessCalendarService,
            BranchAuthorizationService branchAuthorizationService,
            RevenueCashLinkageService revenueCashLinkageService
    ) {
        this.dailyRevenueService = dailyRevenueService;
        this.businessCalendarService = businessCalendarService;
        this.branchAuthorizationService = branchAuthorizationService;
        this.revenueCashLinkageService = revenueCashLinkageService;
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER', 'EMPLOYEE')")
    @Idempotent(required = false)
    public ResponseEntity<DailyRevenueReportDto> createReport(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateDailyRevenueCommand command
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        DailyRevenueReportDto created = dailyRevenueService.createReport(branchId, command, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<DailyRevenueListResponse> getDailyRevenueReports(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) RevenueReportStatus status,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        DailyRevenueListResponse response = dailyRevenueService.getDailyRevenueReports(
                branchId,
                startDate,
                endDate,
                status,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<DailyRevenueReportDto> getDailyRevenueReport(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        DailyRevenueReportDto report = dailyRevenueService.getDailyRevenueReportById(branchId, id);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/{id}/ledger-entries")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<CashLedgerEntryDto>> getRevenueLedgerEntries(
            @PathVariable UUID branchId,
            @PathVariable UUID id
    ) {
        dailyRevenueService.getDailyRevenueReportById(branchId, id);
        List<CashLedgerEntryDto> entries = revenueCashLinkageService.getRevenueLedgerEntries(branchId, id);
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')")
    @Idempotent(required = false)
    public ResponseEntity<DailyRevenueReportDto> confirmReport(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmRevenueCommand command
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        DailyRevenueReportDto confirmed = dailyRevenueService.confirmReport(branchId, id, command, actorUserId);
        return ResponseEntity.ok(confirmed);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER', 'EMPLOYEE')")
    public ResponseEntity<DailyRevenueReportDto> updateReport(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateDailyRevenueCommand command
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        DailyRevenueReportDto updated = dailyRevenueService.updateReport(branchId, id, command, actorUserId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/missing-dates")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<LocalDate>> getMissingDates(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String month
    ) {
        LocalDate[] dates = resolveDateRange(startDate, endDate, month);
        List<LocalDate> missing = businessCalendarService.getMissingBusinessDates(branchId, dates[0], dates[1]);
        return ResponseEntity.ok(missing);
    }

    private LocalDate[] resolveDateRange(LocalDate startDate, LocalDate endDate, String month) {
        if (month != null && !month.trim().isEmpty()) {
            YearMonth ym = YearMonth.parse(month.trim());
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            return new LocalDate[]{
                    startDate != null ? startDate : start,
                    endDate != null ? endDate : end
            };
        }
        return new LocalDate[]{startDate, endDate};
    }
}
