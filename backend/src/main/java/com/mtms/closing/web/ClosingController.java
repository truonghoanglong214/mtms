package com.mtms.closing.web;

import com.mtms.closing.application.ClosingService;
import com.mtms.closing.application.dto.ClosingDtos.ClosePeriodCommand;
import com.mtms.closing.application.dto.ClosingDtos.CloseStatusDto;
import com.mtms.closing.application.dto.ClosingDtos.ClosingPrerequisitesDto;
import com.mtms.closing.application.dto.ClosingDtos.ManagerialPnLReportDto;
import com.mtms.closing.application.dto.ClosingDtos.MonthlyCloseDto;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/closing")
public class ClosingController {

    private final ClosingService closingService;
    private final BranchAuthorizationService branchAuthorizationService;

    public ClosingController(
            ClosingService closingService,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.closingService = closingService;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    @GetMapping("/prerequisites")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<ClosingPrerequisitesDto> getPrerequisites(
            @PathVariable UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ResponseEntity.ok(closingService.checkPrerequisites(branchId, month));
    }

    @GetMapping("/pnl")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<ManagerialPnLReportDto> getManagerialPnL(
            @PathVariable UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ResponseEntity.ok(closingService.calculateManagerialPnL(branchId, month));
    }

    @PostMapping("/close")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'ADMIN', 'OWNER')")
    public ResponseEntity<MonthlyCloseDto> closePeriod(
            @PathVariable UUID branchId,
            @Valid @RequestBody ClosePeriodCommand command
    ) {
        UUID actorUserId = branchAuthorizationService.getCurrentUserId();
        return ResponseEntity.ok(closingService.closePeriod(
                branchId,
                command.monthStart(),
                command.note(),
                actorUserId
        ));
    }

    @GetMapping("/status")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<CloseStatusDto> getCloseStatus(
            @PathVariable UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month
    ) {
        return ResponseEntity.ok(closingService.getCloseStatus(branchId, month));
    }

    @GetMapping("/history")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<MonthlyCloseDto>> getCloseHistory(@PathVariable UUID branchId) {
        return ResponseEntity.ok(closingService.getCloseHistory(branchId));
    }
}
