package com.mtms.revenue.web;

import com.mtms.common.error.UnauthorizedException;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.revenue.application.dto.BusinessDayDto;
import com.mtms.revenue.application.dto.SetBusinessDayCommand;
import com.mtms.revenue.application.service.BusinessCalendarService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * P0-REV-01 / BR-REV-02 / TC-004 / TC-005
 * REST Controller for branch business calendar and day operation status management.
 */
@RestController
@RequestMapping("/api/v1/branches/{branchId}/business-days")
public class BusinessDayController {

    private final BusinessCalendarService businessCalendarService;
    private final BranchAuthorizationService branchAuthorizationService;

    public BusinessDayController(
            BusinessCalendarService businessCalendarService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.businessCalendarService = businessCalendarService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<BusinessDayDto>> getBusinessDays(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String month
    ) {
        LocalDate[] dates = resolveDateRange(startDate, endDate, month);
        List<BusinessDayDto> days = businessCalendarService.getBusinessDays(branchId, dates[0], dates[1]);
        return ResponseEntity.ok(days);
    }

    @PutMapping("/{businessDate}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')")
    public ResponseEntity<BusinessDayDto> setBusinessDay(
            @PathVariable UUID branchId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @Valid @RequestBody SetBusinessDayCommand command
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        if (actorUserId == null) {
            throw new UnauthorizedException("User is unauthenticated");
        }

        BusinessDayDto result = businessCalendarService.setBusinessDay(
                branchId,
                businessDate,
                command.operationStatus(),
                command.closeReason(),
                actorUserId
        );
        return ResponseEntity.ok(result);
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
