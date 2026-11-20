package com.mtms.hr.application;

import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.common.idempotency.CurrentActorProvider;
import com.mtms.hr.application.dto.HrDtos.PagedResponse;
import com.mtms.hr.application.dto.TimesheetDtos.ApproveTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.BulkApproveCommand;
import com.mtms.hr.application.dto.TimesheetDtos.BulkApproveResponse;
import com.mtms.hr.application.dto.TimesheetDtos.BulkTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.BulkTimesheetItemCommand;
import com.mtms.hr.application.dto.TimesheetDtos.CreateTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.MonthlyTimesheetSummaryDto;
import com.mtms.hr.application.dto.TimesheetDtos.RejectTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.TimesheetEntryDto;
import com.mtms.hr.domain.HrExceptions.EmployeeNotFoundException;
import com.mtms.hr.domain.HrExceptions.InvalidTimesheetDataException;
import com.mtms.hr.domain.HrExceptions.SelfApprovalDeniedException;
import com.mtms.hr.domain.HrExceptions.TimesheetAlreadyApprovedException;
import com.mtms.hr.domain.HrExceptions.TimesheetNotFoundException;
import com.mtms.hr.domain.TimesheetSourceType;
import com.mtms.hr.domain.TimesheetStatus;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.hr.infrastructure.persistence.TimesheetEntryJpaEntity;
import com.mtms.hr.infrastructure.persistence.TimesheetEntryJpaRepository;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-HR-03 & P0-HR-04 / BR-HR-02 / TC-013 / TC-017
 * Application service for Timesheet Ingestion, Review, and Approval Lifecycle.
 */
@Service
@Transactional
public class TimesheetService {

    private final TimesheetEntryJpaRepository timesheetRepository;
    private final EmployeeJpaRepository employeeRepository;
    private final BranchJpaRepository branchRepository;
    private final UserJpaRepository userRepository;
    private final ScopedRoleGrantJpaRepository grantRepository;
    private final BranchAuthorizationService branchAuthorizationService;
    private final CurrentActorProvider currentActorProvider;

    public TimesheetService(
            TimesheetEntryJpaRepository timesheetRepository,
            EmployeeJpaRepository employeeRepository,
            BranchJpaRepository branchRepository,
            UserJpaRepository userRepository,
            ScopedRoleGrantJpaRepository grantRepository,
            BranchAuthorizationService branchAuthorizationService,
            @Autowired(required = false) CurrentActorProvider currentActorProvider
    ) {
        this.timesheetRepository = timesheetRepository;
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.grantRepository = grantRepository;
        this.branchAuthorizationService = branchAuthorizationService;
        this.currentActorProvider = currentActorProvider;
    }

    /**
     * Creates or updates a timesheet entry for an employee on a specific work date.
     */
    public TimesheetEntryDto createOrUpdateTimesheet(UUID branchId, CreateTimesheetCommand command) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }
        if (!employeeRepository.existsById(command.employeeId())) {
            throw new EmployeeNotFoundException("Không tìm thấy nhân viên: " + command.employeeId());
        }

        validateTimesheetData(command.rawMinutes(), command.approvedMinutes(), command.payableWorkdays());

        Optional<TimesheetEntryJpaEntity> existingOpt =
                timesheetRepository.findByEmployeeIdAndBranchIdAndWorkDate(
                        command.employeeId(), branchId, command.workDate()
                );

        TimesheetEntryJpaEntity entry;
        if (existingOpt.isPresent()) {
            entry = existingOpt.get();
            if (entry.getStatus() == TimesheetStatus.APPROVED) {
                throw new TimesheetAlreadyApprovedException(
                        "Bản ghi chấm công ngày " + command.workDate() + " đã được phê duyệt, không thể chỉnh sửa."
                );
            }
            entry.setRawMinutes(command.rawMinutes());
            entry.setApprovedMinutes(command.approvedMinutes());
            entry.setPayableWorkdays(command.payableWorkdays());
            if (command.status() != null) {
                entry.setStatus(command.status());
            }
            entry.setNote(command.note());
            entry.setUpdatedAt(Instant.now());
        } else {
            entry = new TimesheetEntryJpaEntity(
                    UUID.randomUUID(),
                    command.employeeId(),
                    branchId,
                    command.workDate(),
                    command.rawMinutes(),
                    command.approvedMinutes(),
                    command.payableWorkdays(),
                    TimesheetSourceType.MANUAL,
                    command.status() != null ? command.status() : TimesheetStatus.DRAFT,
                    command.note(),
                    null,
                    null,
                    null,
                    Instant.now(),
                    Instant.now()
            );
        }

        TimesheetEntryJpaEntity saved = timesheetRepository.save(entry);
        return mapToDto(saved);
    }

    /**
     * Bulk creates/updates timesheet entries for multiple employees on a work date.
     */
    public List<TimesheetEntryDto> bulkCreateTimesheet(UUID branchId, BulkTimesheetCommand command) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }
        List<TimesheetEntryDto> results = new ArrayList<>();
        for (BulkTimesheetItemCommand item : command.entries()) {
            CreateTimesheetCommand singleCommand = new CreateTimesheetCommand(
                    item.employeeId(),
                    command.workDate(),
                    item.rawMinutes(),
                    item.approvedMinutes(),
                    item.payableWorkdays(),
                    item.status(),
                    item.note()
            );
            results.add(createOrUpdateTimesheet(branchId, singleCommand));
        }
        return results;
    }

    /**
     * Approves a single timesheet entry with strict TC-017 self-approval invariant check.
     */
    public TimesheetEntryDto approveTimesheet(UUID branchId, UUID timesheetId, ApproveTimesheetCommand command) {
        TimesheetEntryJpaEntity entry = timesheetRepository.findByIdAndBranchId(timesheetId, branchId)
                .orElseThrow(() -> new TimesheetNotFoundException("Không tìm thấy dòng chấm công ID: " + timesheetId));

        UUID currentUserId = getCurrentUserId();
        EmployeeJpaEntity employee = employeeRepository.findById(entry.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + entry.getEmployeeId()));

        // TC-017 Invariant: Manager cannot approve their own timesheet entry
        if (employee.getUserId() != null && employee.getUserId().equals(currentUserId)) {
            boolean isOwnerOrAdmin = isBranchOwnerOrAdmin(currentUserId, branchId);
            if (!isOwnerOrAdmin) {
                throw new SelfApprovalDeniedException("Quản lý không được phép tự phê duyệt giờ công của chính mình.");
            }
        }

        entry.setStatus(TimesheetStatus.APPROVED);
        entry.setApprovedBy(currentUserId);
        entry.setApprovedAt(Instant.now());
        if (command != null && command.approvalNote() != null) {
            entry.setApprovalNote(command.approvalNote());
        }
        entry.setUpdatedAt(Instant.now());

        TimesheetEntryJpaEntity saved = timesheetRepository.save(entry);
        return mapToDto(saved);
    }

    /**
     * Bulk approves timesheet entries with atomic TC-017 enforcement.
     */
    public BulkApproveResponse bulkApprove(UUID branchId, BulkApproveCommand command) {
        int approvedCount = 0;
        ApproveTimesheetCommand approveCmd = new ApproveTimesheetCommand(command.approvalNote());
        for (UUID id : command.entryIds()) {
            approveTimesheet(branchId, id, approveCmd);
            approvedCount++;
        }
        return new BulkApproveResponse(approvedCount, 0);
    }

    /**
     * Rejects a timesheet entry with rejection note.
     */
    public TimesheetEntryDto rejectTimesheet(UUID branchId, UUID timesheetId, RejectTimesheetCommand command) {
        TimesheetEntryJpaEntity entry = timesheetRepository.findByIdAndBranchId(timesheetId, branchId)
                .orElseThrow(() -> new TimesheetNotFoundException("Không tìm thấy dòng chấm công ID: " + timesheetId));

        entry.setStatus(TimesheetStatus.REJECTED);
        if (command != null && command.rejectionReason() != null) {
            entry.setApprovalNote(command.rejectionReason());
        }
        entry.setUpdatedAt(Instant.now());

        TimesheetEntryJpaEntity saved = timesheetRepository.save(entry);
        return mapToDto(saved);
    }

    /**
     * Retrieves paginated timesheets for a branch with optional date range, employee, and status filters.
     */
    @Transactional(readOnly = true)
    public PagedResponse<TimesheetEntryDto> getTimesheets(
            UUID branchId,
            LocalDate startDate,
            LocalDate endDate,
            UUID employeeId,
            TimesheetStatus status,
            Pageable pageable
    ) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        Page<TimesheetEntryJpaEntity> entityPage = timesheetRepository.findFiltered(
                branchId, startDate, endDate, employeeId, status, pageable
        );

        List<TimesheetEntryDto> dtos = mapToDtoBatch(entityPage.getContent());
        Page<TimesheetEntryDto> dtoPage = new PageImpl<>(dtos, pageable, entityPage.getTotalElements());

        return PagedResponse.from(dtoPage);
    }

    /**
     * Computes monthly timesheet summary per employee for a branch.
     */
    @Transactional(readOnly = true)
    public List<MonthlyTimesheetSummaryDto> getMonthlySummary(UUID branchId, int year, int month) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<TimesheetEntryJpaEntity> entries =
                timesheetRepository.findByBranchIdAndWorkDateBetween(branchId, startDate, endDate);

        Map<UUID, List<TimesheetEntryJpaEntity>> entriesByEmp = entries.stream()
                .collect(Collectors.groupingBy(TimesheetEntryJpaEntity::getEmployeeId));

        List<EmployeeJpaEntity> branchEmployees = employeeRepository.findByBranch(
                branchId, null, null, Pageable.unpaged()
        ).getContent();

        Set<UUID> allEmployeeIds = new LinkedHashSet<>();
        for (EmployeeJpaEntity e : branchEmployees) {
            allEmployeeIds.add(e.getId());
        }
        allEmployeeIds.addAll(entriesByEmp.keySet());

        Map<UUID, EmployeeJpaEntity> empMap = employeeRepository.findAllById(allEmployeeIds)
                .stream().collect(Collectors.toMap(EmployeeJpaEntity::getId, e -> e));

        List<MonthlyTimesheetSummaryDto> summaryList = new ArrayList<>();
        for (UUID empId : allEmployeeIds) {
            EmployeeJpaEntity emp = empMap.get(empId);
            if (emp == null) continue;

            List<TimesheetEntryJpaEntity> empEntries = entriesByEmp.getOrDefault(empId, List.of());

            int totalApprovedMinutes = 0;
            BigDecimal totalPayableWorkdays = BigDecimal.ZERO;
            int approvedCount = 0;
            int pendingCount = 0;
            int rejectedCount = 0;

            for (TimesheetEntryJpaEntity e : empEntries) {
                if (e.getStatus() == TimesheetStatus.APPROVED) {
                    approvedCount++;
                    if (e.getApprovedMinutes() != null) {
                        totalApprovedMinutes += e.getApprovedMinutes();
                    }
                    if (e.getPayableWorkdays() != null) {
                        totalPayableWorkdays = totalPayableWorkdays.add(e.getPayableWorkdays());
                    }
                } else if (e.getStatus() == TimesheetStatus.DRAFT || e.getStatus() == TimesheetStatus.SUBMITTED) {
                    pendingCount++;
                } else if (e.getStatus() == TimesheetStatus.REJECTED) {
                    rejectedCount++;
                }
            }

            BigDecimal totalApprovedHours = BigDecimal.valueOf(totalApprovedMinutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            summaryList.add(new MonthlyTimesheetSummaryDto(
                    emp.getId(),
                    emp.getStaffCode(),
                    emp.getFullName(),
                    emp.getPositionName(),
                    totalApprovedMinutes,
                    totalApprovedHours,
                    totalPayableWorkdays,
                    approvedCount,
                    pendingCount,
                    rejectedCount
            ));
        }

        summaryList.sort(Comparator.comparing(MonthlyTimesheetSummaryDto::staffCode));
        return summaryList;
    }

    private void validateTimesheetData(Integer rawMinutes, Integer approvedMinutes, BigDecimal payableWorkdays) {
        if (approvedMinutes != null && approvedMinutes < 0) {
            throw new InvalidTimesheetDataException("Số phút duyệt (approvedMinutes) không được âm: " + approvedMinutes);
        }
        if (payableWorkdays != null && payableWorkdays.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTimesheetDataException("Số ngày công tính lương (payableWorkdays) không được âm: " + payableWorkdays);
        }
        if (rawMinutes != null && rawMinutes < 0) {
            throw new InvalidTimesheetDataException("Số phút thô (rawMinutes) không được âm: " + rawMinutes);
        }
    }

    private boolean isBranchOwnerOrAdmin(UUID currentUserId, UUID branchId) {
        if (currentUserId == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (grantRepository.isSystemAdmin(currentUserId, today)) {
            return true;
        }
        List<ScopedRoleGrantJpaEntity> grants =
                grantRepository.findActiveGrantsByUserIdAndBranchId(currentUserId, branchId, today);
        for (ScopedRoleGrantJpaEntity grant : grants) {
            String role = grant.getRoleKey();
            if (role != null) {
                String upper = role.trim().toUpperCase();
                if ("BRANCH_OWNER".equals(upper) || "SYSTEM_ADMIN".equals(upper) || upper.contains("OWNER") || upper.contains("ADMIN")) {
                    return true;
                }
            }
        }
        return false;
    }

    private UUID getCurrentUserId() {
        if (currentActorProvider != null) {
            UUID actorId = currentActorProvider.getCurrentActorId();
            if (actorId != null) {
                return actorId;
            }
        }
        if (branchAuthorizationService != null) {
            return branchAuthorizationService.getCurrentUserId();
        }
        return null;
    }

    private TimesheetEntryDto mapToDto(TimesheetEntryJpaEntity entry) {
        EmployeeJpaEntity emp = employeeRepository.findById(entry.getEmployeeId()).orElse(null);
        String staffCode = emp != null ? emp.getStaffCode() : "";
        String employeeName = emp != null ? emp.getFullName() : "";
        String positionName = emp != null ? emp.getPositionName() : "";

        String approverName = null;
        if (entry.getApprovedBy() != null) {
            UserJpaEntity approver = userRepository.findById(entry.getApprovedBy()).orElse(null);
            if (approver != null) {
                approverName = approver.getDisplayName();
            }
        }

        return new TimesheetEntryDto(
                entry.getId(),
                entry.getEmployeeId(),
                staffCode,
                employeeName,
                positionName,
                entry.getWorkDate(),
                entry.getRawMinutes(),
                entry.getApprovedMinutes(),
                entry.getPayableWorkdays(),
                entry.getSourceType().name(),
                entry.getStatus(),
                entry.getNote(),
                entry.getApprovedBy(),
                approverName,
                entry.getApprovedAt(),
                entry.getApprovalNote()
        );
    }

    private List<TimesheetEntryDto> mapToDtoBatch(List<TimesheetEntryJpaEntity> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }

        Set<UUID> empIds = entries.stream().map(TimesheetEntryJpaEntity::getEmployeeId).collect(Collectors.toSet());
        Set<UUID> approverIds = entries.stream()
                .map(TimesheetEntryJpaEntity::getApprovedBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, EmployeeJpaEntity> empMap = employeeRepository.findAllById(empIds).stream()
                .collect(Collectors.toMap(EmployeeJpaEntity::getId, e -> e));

        Map<UUID, UserJpaEntity> userMap = userRepository.findAllById(approverIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, u -> u));

        return entries.stream().map(entry -> {
            EmployeeJpaEntity emp = empMap.get(entry.getEmployeeId());
            String staffCode = emp != null ? emp.getStaffCode() : "";
            String employeeName = emp != null ? emp.getFullName() : "";
            String positionName = emp != null ? emp.getPositionName() : "";

            String approverName = null;
            if (entry.getApprovedBy() != null) {
                UserJpaEntity approver = userMap.get(entry.getApprovedBy());
                if (approver != null) {
                    approverName = approver.getDisplayName();
                }
            }

            return new TimesheetEntryDto(
                    entry.getId(),
                    entry.getEmployeeId(),
                    staffCode,
                    employeeName,
                    positionName,
                    entry.getWorkDate(),
                    entry.getRawMinutes(),
                    entry.getApprovedMinutes(),
                    entry.getPayableWorkdays(),
                    entry.getSourceType().name(),
                    entry.getStatus(),
                    entry.getNote(),
                    entry.getApprovedBy(),
                    approverName,
                    entry.getApprovedAt(),
                    entry.getApprovalNote()
            );
        }).collect(Collectors.toList());
    }
}
