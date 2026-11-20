package com.mtms.hr.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.hr.application.dto.PayrollDtos.CalculationSnapshotDto;
import com.mtms.hr.application.dto.PayrollDtos.CreateAdjustmentCommand;
import com.mtms.hr.application.dto.PayrollDtos.PayrollAdjustmentDto;
import com.mtms.hr.application.dto.PayrollDtos.PayrollLineDto;
import com.mtms.hr.application.dto.PayrollDtos.PayrollPeriodDto;
import com.mtms.hr.application.dto.PayrollDtos.PolicySnapshotDto;
import com.mtms.hr.application.dto.PayrollDtos.RateSliceDto;
import com.mtms.hr.domain.HrExceptions.EmployeeNotFoundException;
import com.mtms.hr.domain.HrExceptions.SelfApprovalDeniedException;
import com.mtms.hr.domain.InvalidPayrollDataException;
import com.mtms.hr.domain.PayrollAdjustmentNotFoundException;
import com.mtms.hr.domain.PayrollAlreadyApprovedException;
import com.mtms.hr.domain.PayrollLineNotFoundException;
import com.mtms.hr.domain.PayrollPeriodNotFoundException;
import com.mtms.hr.domain.PayrollPeriodStatus;
import com.mtms.hr.domain.RateType;
import com.mtms.hr.domain.TimesheetStatus;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollAdjustmentJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollAdjustmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollLineJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollLineJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaRepository;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaEntity;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaRepository;
import com.mtms.hr.infrastructure.persistence.TimesheetEntryJpaEntity;
import com.mtms.hr.infrastructure.persistence.TimesheetEntryJpaRepository;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-HR-05 & P0-HR-06
 * Core Payroll Calculation Engine, Time-sliced Wage Processing, and Invariant Approval Service.
 * Implements BR-HR-04, BR-HR-05, BR-HR-10, TC-013, TC-014, TC-015, TC-017.
 */
@Service
public class PayrollService {

    private final PayrollPeriodJpaRepository periodRepository;
    private final PayrollLineJpaRepository lineRepository;
    private final PayrollAdjustmentJpaRepository adjustmentRepository;
    private final EmployeeJpaRepository employeeRepository;
    private final EmployeeAssignmentJpaRepository assignmentRepository;
    private final SalaryPolicyJpaRepository salaryPolicyRepository;
    private final TimesheetEntryJpaRepository timesheetRepository;
    private final BranchJpaRepository branchRepository;
    private final ScopedRoleGrantJpaRepository grantRepository;
    private final BranchAuthorizationService branchAuth;
    private final ObjectMapper objectMapper;

    public PayrollService(
            PayrollPeriodJpaRepository periodRepository,
            PayrollLineJpaRepository lineRepository,
            PayrollAdjustmentJpaRepository adjustmentRepository,
            EmployeeJpaRepository employeeRepository,
            EmployeeAssignmentJpaRepository assignmentRepository,
            SalaryPolicyJpaRepository salaryPolicyRepository,
            TimesheetEntryJpaRepository timesheetRepository,
            BranchJpaRepository branchRepository,
            ScopedRoleGrantJpaRepository grantRepository,
            BranchAuthorizationService branchAuth,
            ObjectMapper objectMapper
    ) {
        this.periodRepository = periodRepository;
        this.lineRepository = lineRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.salaryPolicyRepository = salaryPolicyRepository;
        this.timesheetRepository = timesheetRepository;
        this.branchRepository = branchRepository;
        this.grantRepository = grantRepository;
        this.branchAuth = branchAuth;
        this.objectMapper = objectMapper;
    }

    /**
     * Gets current payroll period for month. If absent, automatically generates a draft period.
     */
    @Transactional
    public PayrollPeriodDto getCurrentPeriod(UUID branchId, LocalDate monthStart) {
        if (monthStart == null) {
            monthStart = LocalDate.now().withDayOfMonth(1);
        }
        validateMonthStart(monthStart);

        Optional<PayrollPeriodJpaEntity> periodOpt = periodRepository.findByBranchIdAndMonthStartAndIsCurrentTrue(branchId, monthStart);
        if (periodOpt.isPresent()) {
            return mapToDto(periodOpt.get());
        }

        return generateOrRecalculatePayroll(branchId, monthStart);
    }

    /**
     * Lists payroll periods with optional year and status filter.
     */
    @Transactional(readOnly = true)
    public List<PayrollPeriodDto> getPeriods(UUID branchId, Integer year, PayrollPeriodStatus status) {
        List<PayrollPeriodJpaEntity> entities = periodRepository.findFiltered(branchId, year, status);
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Retrieves specific payroll period by id.
     */
    @Transactional(readOnly = true)
    public PayrollPeriodDto getPeriodById(UUID branchId, UUID periodId) {
        PayrollPeriodJpaEntity period = periodRepository.findByIdAndBranchId(periodId, branchId)
                .orElseThrow(() -> new PayrollPeriodNotFoundException("Không tìm thấy kỳ bảng lương ID: " + periodId));
        return mapToDto(period);
    }

    /**
     * Retrieves lines of a specific payroll period.
     */
    @Transactional(readOnly = true)
    public List<PayrollLineDto> getPeriodLines(UUID branchId, UUID periodId) {
        PayrollPeriodJpaEntity period = periodRepository.findByIdAndBranchId(periodId, branchId)
                .orElseThrow(() -> new PayrollPeriodNotFoundException("Không tìm thấy kỳ bảng lương ID: " + periodId));
        return getLineDtosForPeriod(period.getId());
    }

    /**
     * Calculates or recalculates payroll period from approved timesheets.
     * Enforces TC-013 (multi-slice hourly wage), TC-014 (standard workday proration),
     * and TC-015 (month-end primary branch allocation).
     */
    @Transactional
    public PayrollPeriodDto generateOrRecalculatePayroll(UUID branchId, LocalDate monthStart) {
        validateMonthStart(monthStart);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        Optional<PayrollPeriodJpaEntity> currentOpt = periodRepository.findByBranchIdAndMonthStartAndIsCurrentTrue(branchId, monthStart);
        PayrollPeriodJpaEntity period;

        if (currentOpt.isPresent()) {
            period = currentOpt.get();
            if (period.getStatus() == PayrollPeriodStatus.APPROVED) {
                throw new PayrollAlreadyApprovedException("Bảng lương tháng " + monthStart + " đã được phê duyệt, không thể tính lại.");
            }
        } else {
            period = new PayrollPeriodJpaEntity(
                    UUID.randomUUID(),
                    branchId,
                    monthStart,
                    1,
                    true,
                    PayrollPeriodStatus.DRAFT,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    null,
                    null,
                    null,
                    Instant.now(),
                    Instant.now()
            );
            period = periodRepository.save(period);
        }

        // TC-015: Find employees whose is_primary = true at this branch on monthEnd
        List<EmployeeAssignmentJpaEntity> primaryAssignments = assignmentRepository.findActivePrimaryAssignmentsByBranchOnDate(branchId, monthEnd);
        Set<UUID> targetEmployeeIds = primaryAssignments.stream()
                .map(EmployeeAssignmentJpaEntity::getEmployeeId)
                .collect(Collectors.toSet());

        // Load existing lines for this period to retain adjustments
        List<PayrollLineJpaEntity> existingLines = lineRepository.findByPayrollPeriodId(period.getId());
        Map<UUID, PayrollLineJpaEntity> existingLineMap = existingLines.stream()
                .collect(Collectors.toMap(PayrollLineJpaEntity::getEmployeeId, l -> l));

        // Delete lines for employees no longer primary on monthEnd
        for (PayrollLineJpaEntity oldLine : existingLines) {
            if (!targetEmployeeIds.contains(oldLine.getEmployeeId())) {
                lineRepository.delete(oldLine);
            }
        }

        List<PayrollLineJpaEntity> updatedLines = new ArrayList<>();
        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalAdjustments = BigDecimal.ZERO;
        BigDecimal totalGrossSalary = BigDecimal.ZERO;

        for (EmployeeAssignmentJpaEntity assignment : primaryAssignments) {
            UUID employeeId = assignment.getEmployeeId();
            EmployeeJpaEntity employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                continue;
            }
            if (employee.getLeftOn() != null && employee.getLeftOn().isBefore(monthStart)) {
                continue;
            }

            // Fetch all approved timesheets in the month
            List<TimesheetEntryJpaEntity> timesheets = timesheetRepository.findByEmployeeIdAndDateRangeAndStatus(
                    employeeId, monthStart, monthEnd, TimesheetStatus.APPROVED
            );

            // Fetch policies for this employee
            SalaryPolicyJpaEntity latestPolicy = findLatestEffectivePolicy(employeeId, branchId, monthEnd, monthStart);
            if (latestPolicy == null) {
                throw new InvalidPayrollDataException("Không tìm thấy chính sách lương cho nhân viên: " + employee.getStaffCode());
            }

            RateType rateType = latestPolicy.getRateType();
            int approvedMinutes = 0;
            BigDecimal payableWorkdays = BigDecimal.ZERO;
            BigDecimal baseSalary = BigDecimal.ZERO;
            String policySnapshotJson;
            String calculationSnapshotJson;

            if (rateType == RateType.HOURLY) {
                // TC-013: Hourly Multi-slice Wage Calculation
                List<RateSliceDto> slices = calculateHourlySlices(employeeId, branchId, monthStart, monthEnd, timesheets);
                for (RateSliceDto slice : slices) {
                    baseSalary = baseSalary.add(slice.amount());
                    approvedMinutes += (int) Math.round(slice.hours() * 60.0);
                }

                PolicySnapshotDto policyDto = new PolicySnapshotDto(
                        RateType.HOURLY.name(),
                        latestPolicy.getRateAmount(),
                        null
                );
                CalculationSnapshotDto calcDto = new CalculationSnapshotDto(
                        slices, null, null, null, null
                );

                policySnapshotJson = toJson(policyDto);
                calculationSnapshotJson = toJson(calcDto);
            } else {
                // TC-014: Monthly Wage Proration by Standard Workdays
                BigDecimal standardWorkdays = latestPolicy.getStandardWorkdays() != null
                        ? latestPolicy.getStandardWorkdays()
                        : BigDecimal.valueOf(26.00);

                for (TimesheetEntryJpaEntity ts : timesheets) {
                    approvedMinutes += ts.getApprovedMinutes() != null ? ts.getApprovedMinutes() : 0;
                    payableWorkdays = payableWorkdays.add(ts.getPayableWorkdays() != null ? ts.getPayableWorkdays() : BigDecimal.ZERO);
                }

                // base_salary = rate * (payable_workdays / standard_workdays)
                baseSalary = latestPolicy.getRateAmount()
                        .multiply(payableWorkdays)
                        .divide(standardWorkdays, 2, RoundingMode.HALF_UP);

                PolicySnapshotDto policyDto = new PolicySnapshotDto(
                        RateType.MONTHLY.name(),
                        latestPolicy.getRateAmount(),
                        standardWorkdays
                );
                CalculationSnapshotDto calcDto = new CalculationSnapshotDto(
                        null,
                        standardWorkdays,
                        payableWorkdays,
                        latestPolicy.getRateAmount(),
                        baseSalary
                );

                policySnapshotJson = toJson(policyDto);
                calculationSnapshotJson = toJson(calcDto);
            }

            // Get or create line
            PayrollLineJpaEntity line = existingLineMap.get(employeeId);
            if (line == null) {
                line = new PayrollLineJpaEntity(
                        UUID.randomUUID(),
                        period.getId(),
                        employeeId,
                        branchId,
                        rateType,
                        approvedMinutes,
                        payableWorkdays,
                        baseSalary,
                        BigDecimal.ZERO,
                        baseSalary,
                        BigDecimal.ZERO,
                        policySnapshotJson,
                        calculationSnapshotJson,
                        Instant.now(),
                        Instant.now()
                );
                line = lineRepository.save(line);
            } else {
                line.setExpenseBranchId(branchId);
                line.setRateType(rateType);
                line.setApprovedMinutes(approvedMinutes);
                line.setPayableWorkdays(payableWorkdays);
                line.setBaseSalary(baseSalary);
                line.setPolicySnapshot(policySnapshotJson);
                line.setCalculationSnapshot(calculationSnapshotJson);
            }

            // Calculate adjustments and gross
            List<PayrollAdjustmentJpaEntity> adjs = adjustmentRepository.findByPayrollLineId(line.getId());
            BigDecimal adjTotal = adjs.stream()
                    .map(PayrollAdjustmentJpaEntity::getSignedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal gross = baseSalary.add(adjTotal);
            if (gross.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidPayrollDataException("Lương tổng (gross salary) không được âm cho nhân viên: " + employee.getStaffCode());
            }

            line.setAdjustmentsTotal(adjTotal);
            line.setGrossSalary(gross);
            line.setUpdatedAt(Instant.now());
            line = lineRepository.save(line);

            updatedLines.add(line);
            totalBaseSalary = totalBaseSalary.add(baseSalary);
            totalAdjustments = totalAdjustments.add(adjTotal);
            totalGrossSalary = totalGrossSalary.add(gross);
        }

        period.setTotalBaseSalary(totalBaseSalary);
        period.setTotalAdjustments(totalAdjustments);
        period.setTotalGrossSalary(totalGrossSalary);
        period.setEmployeeCount(updatedLines.size());
        period.setUpdatedAt(Instant.now());
        PayrollPeriodJpaEntity savedPeriod = periodRepository.save(period);

        return mapToDto(savedPeriod);
    }

    /**
     * Adds manual adjustment to a draft payroll line.
     */
    @Transactional
    public PayrollAdjustmentDto addAdjustment(UUID branchId, UUID lineId, CreateAdjustmentCommand command) {
        if (command == null || command.signedAmount() == null || command.signedAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidPayrollDataException("Số tiền điều chỉnh phải khác 0");
        }
        if (command.explanation() == null || command.explanation().isBlank()) {
            throw new InvalidPayrollDataException("Lý do điều chỉnh không được để trống");
        }

        PayrollLineJpaEntity line = lineRepository.findById(lineId)
                .orElseThrow(() -> new PayrollLineNotFoundException("Không tìm thấy dòng lương ID: " + lineId));

        PayrollPeriodJpaEntity period = periodRepository.findByIdAndBranchId(line.getPayrollPeriodId(), branchId)
                .orElseThrow(() -> new PayrollPeriodNotFoundException("Không tìm thấy kỳ bảng lương của chi nhánh: " + branchId));

        if (period.getStatus() != PayrollPeriodStatus.DRAFT) {
            throw new PayrollAlreadyApprovedException("Không thể thêm điều chỉnh khi bảng lương không ở trạng thái DRAFT");
        }

        BigDecimal newAdjustmentsTotal = line.getAdjustmentsTotal().add(command.signedAmount());
        BigDecimal newGrossSalary = line.getBaseSalary().add(newAdjustmentsTotal);
        if (newGrossSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPayrollDataException("Lương tổng sau điều chỉnh không được âm");
        }

        UUID currentUserId = branchAuth.getCurrentUserId();
        PayrollAdjustmentJpaEntity adj = new PayrollAdjustmentJpaEntity(
                UUID.randomUUID(),
                lineId,
                command.adjustmentCode(),
                command.signedAmount(),
                command.explanation().trim(),
                currentUserId,
                Instant.now()
        );
        adj = adjustmentRepository.save(adj);

        line.setAdjustmentsTotal(newAdjustmentsTotal);
        line.setGrossSalary(newGrossSalary);
        line.setUpdatedAt(Instant.now());
        lineRepository.save(line);

        period.setTotalAdjustments(period.getTotalAdjustments().add(command.signedAmount()));
        period.setTotalGrossSalary(period.getTotalGrossSalary().add(command.signedAmount()));
        period.setUpdatedAt(Instant.now());
        periodRepository.save(period);

        return new PayrollAdjustmentDto(
                adj.getId(),
                adj.getPayrollLineId(),
                adj.getAdjustmentCode().name(),
                adj.getSignedAmount(),
                adj.getExplanation(),
                adj.getApprovedBy(),
                adj.getCreatedAt()
        );
    }

    /**
     * Deletes a manual adjustment from a draft payroll line.
     */
    @Transactional
    public void deleteAdjustment(UUID branchId, UUID adjId) {
        PayrollAdjustmentJpaEntity adj = adjustmentRepository.findById(adjId)
                .orElseThrow(() -> new PayrollAdjustmentNotFoundException("Không tìm thấy khoản điều chỉnh ID: " + adjId));

        PayrollLineJpaEntity line = lineRepository.findById(adj.getPayrollLineId())
                .orElseThrow(() -> new PayrollLineNotFoundException("Không tìm thấy dòng lương ID: " + adj.getPayrollLineId()));

        PayrollPeriodJpaEntity period = periodRepository.findByIdAndBranchId(line.getPayrollPeriodId(), branchId)
                .orElseThrow(() -> new PayrollPeriodNotFoundException("Không tìm thấy kỳ bảng lương của chi nhánh: " + branchId));

        if (period.getStatus() != PayrollPeriodStatus.DRAFT) {
            throw new PayrollAlreadyApprovedException("Không thể xóa điều chỉnh khi bảng lương không ở trạng thái DRAFT");
        }

        BigDecimal newAdjustmentsTotal = line.getAdjustmentsTotal().subtract(adj.getSignedAmount());
        BigDecimal newGrossSalary = line.getBaseSalary().add(newAdjustmentsTotal);
        if (newGrossSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPayrollDataException("Lương tổng sau khi xóa điều chỉnh không được âm");
        }

        adjustmentRepository.delete(adj);

        line.setAdjustmentsTotal(newAdjustmentsTotal);
        line.setGrossSalary(newGrossSalary);
        line.setUpdatedAt(Instant.now());
        lineRepository.save(line);

        period.setTotalAdjustments(period.getTotalAdjustments().subtract(adj.getSignedAmount()));
        period.setTotalGrossSalary(period.getTotalGrossSalary().subtract(adj.getSignedAmount()));
        period.setUpdatedAt(Instant.now());
        periodRepository.save(period);
    }

    /**
     * Approves payroll period, locking snapshots permanently.
     * Enforces TC-017 / HR-INV-02: Only Branch Owner or System Admin can approve.
     * Store manager is rejected with 403 Forbidden (SelfApprovalDeniedException).
     */
    @Transactional
    public PayrollPeriodDto approvePayrollPeriod(UUID branchId, UUID periodId, UUID currentUserId) {
        PayrollPeriodJpaEntity period = periodRepository.findByIdAndBranchId(periodId, branchId)
                .orElseThrow(() -> new PayrollPeriodNotFoundException("Không tìm thấy kỳ bảng lương ID: " + periodId));

        if (period.getStatus() == PayrollPeriodStatus.APPROVED) {
            throw new PayrollAlreadyApprovedException("Bảng lương đã được phê duyệt trước đó.");
        }

        // TC-017: Verify approver authority
        if (!isBranchOwnerOrAdmin(currentUserId, branchId)) {
            throw new SelfApprovalDeniedException("Chỉ Chủ quán hoặc Quản trị viên mới có thẩm quyền phê duyệt bảng lương. Quản lý cửa hàng không có quyền phê duyệt.");
        }

        period.setStatus(PayrollPeriodStatus.APPROVED);
        period.setApprovedBy(currentUserId);
        period.setApprovedAt(Instant.now());
        period.setUpdatedAt(Instant.now());
        PayrollPeriodJpaEntity saved = periodRepository.save(period);

        return mapToDto(saved);
    }

    // Helper: Calculate hourly wage slices (TC-013)
    private List<RateSliceDto> calculateHourlySlices(
            UUID employeeId,
            UUID branchId,
            LocalDate monthStart,
            LocalDate monthEnd,
            List<TimesheetEntryJpaEntity> timesheets
    ) {
        List<SalaryPolicyJpaEntity> policies = salaryPolicyRepository.findByEmployeeIdOrderByEffectiveFromAscCreatedAtAsc(employeeId);
        List<SalaryPolicyJpaEntity> activePolicies = policies.stream()
                .filter(p -> p.getRateType() == RateType.HOURLY)
                .filter(p -> !p.getEffectiveFrom().isAfter(monthEnd) && (p.getEffectiveTo() == null || !p.getEffectiveTo().isBefore(monthStart)))
                .sorted(Comparator.comparing(SalaryPolicyJpaEntity::getEffectiveFrom))
                .toList();

        if (activePolicies.isEmpty()) {
            throw new InvalidPayrollDataException("Không tìm thấy chính sách lương theo giờ hiệu lực trong tháng cho nhân viên: " + employeeId);
        }

        List<RateSliceDto> slices = new ArrayList<>();
        for (SalaryPolicyJpaEntity policy : activePolicies) {
            LocalDate sliceStart = policy.getEffectiveFrom().isBefore(monthStart) ? monthStart : policy.getEffectiveFrom();
            LocalDate sliceEnd = (policy.getEffectiveTo() == null || policy.getEffectiveTo().isAfter(monthEnd)) ? monthEnd : policy.getEffectiveTo();

            if (sliceStart.isAfter(sliceEnd)) {
                continue;
            }

            int sliceMinutes = 0;
            for (TimesheetEntryJpaEntity ts : timesheets) {
                if (!ts.getWorkDate().isBefore(sliceStart) && !ts.getWorkDate().isAfter(sliceEnd)) {
                    sliceMinutes += ts.getApprovedMinutes() != null ? ts.getApprovedMinutes() : 0;
                }
            }

            double hours = sliceMinutes / 60.0;
            BigDecimal amount = BigDecimal.valueOf(sliceMinutes)
                    .multiply(policy.getRateAmount())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            slices.add(new RateSliceDto(
                    sliceStart,
                    sliceEnd,
                    policy.getRateAmount(),
                    hours,
                    amount
            ));
        }

        return slices;
    }

    private SalaryPolicyJpaEntity findLatestEffectivePolicy(UUID employeeId, UUID branchId, LocalDate onDate, LocalDate fallbackDate) {
        List<SalaryPolicyJpaEntity> list = salaryPolicyRepository.findEffectivePolicies(employeeId, branchId, onDate);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        list = salaryPolicyRepository.findEffectivePoliciesForEmployee(employeeId, onDate);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        list = salaryPolicyRepository.findEffectivePolicies(employeeId, branchId, fallbackDate);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        list = salaryPolicyRepository.findEffectivePoliciesForEmployee(employeeId, fallbackDate);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        List<SalaryPolicyJpaEntity> all = salaryPolicyRepository.findByEmployeeIdOrderByEffectiveFromAscCreatedAtAsc(employeeId);
        if (!all.isEmpty()) {
            return all.get(all.size() - 1);
        }
        return null;
    }

    private boolean isBranchOwnerOrAdmin(UUID currentUserId, UUID branchId) {
        if (currentUserId == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (grantRepository.isSystemAdmin(currentUserId, today)) {
            return true;
        }
        List<ScopedRoleGrantJpaEntity> grants = grantRepository.findActiveGrantsByUserIdAndBranchId(currentUserId, branchId, today);
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

    private void validateMonthStart(LocalDate monthStart) {
        if (monthStart == null || monthStart.getDayOfMonth() != 1) {
            throw new InvalidPayrollDataException("Ngày bắt đầu kỳ lương phải là ngày 1 của tháng: " + monthStart);
        }
    }

    private PayrollPeriodDto mapToDto(PayrollPeriodJpaEntity period) {
        List<PayrollLineDto> lines = getLineDtosForPeriod(period.getId());
        return new PayrollPeriodDto(
                period.getId(),
                period.getBranchId(),
                period.getMonthStart(),
                period.getRevision(),
                period.getIsCurrent(),
                period.getStatus(),
                period.getTotalGrossSalary(),
                period.getTotalBaseSalary(),
                period.getTotalAdjustments(),
                period.getEmployeeCount(),
                period.getNote(),
                period.getApprovedBy(),
                period.getApprovedAt(),
                lines
        );
    }

    private List<PayrollLineDto> getLineDtosForPeriod(UUID periodId) {
        List<PayrollLineJpaEntity> lines = lineRepository.findByPayrollPeriodId(periodId);
        if (lines.isEmpty()) {
            return List.of();
        }

        Set<UUID> empIds = lines.stream().map(PayrollLineJpaEntity::getEmployeeId).collect(Collectors.toSet());
        Map<UUID, EmployeeJpaEntity> empMap = employeeRepository.findAllById(empIds).stream()
                .collect(Collectors.toMap(EmployeeJpaEntity::getId, e -> e));

        List<UUID> lineIds = lines.stream().map(PayrollLineJpaEntity::getId).toList();
        List<PayrollAdjustmentJpaEntity> allAdjs = adjustmentRepository.findByPayrollLineIdIn(lineIds);
        Map<UUID, List<PayrollAdjustmentJpaEntity>> adjMap = allAdjs.stream()
                .collect(Collectors.groupingBy(PayrollAdjustmentJpaEntity::getPayrollLineId));

        return lines.stream().map(line -> {
            EmployeeJpaEntity emp = empMap.get(line.getEmployeeId());
            String staffCode = emp != null ? emp.getStaffCode() : "";
            String employeeName = emp != null ? emp.getFullName() : "";
            String positionName = emp != null ? emp.getPositionName() : "";

            List<PayrollAdjustmentJpaEntity> lineAdjs = adjMap.getOrDefault(line.getId(), List.of());
            List<PayrollAdjustmentDto> adjDtos = lineAdjs.stream().map(adj -> new PayrollAdjustmentDto(
                    adj.getId(),
                    adj.getPayrollLineId(),
                    adj.getAdjustmentCode().name(),
                    adj.getSignedAmount(),
                    adj.getExplanation(),
                    adj.getApprovedBy(),
                    adj.getCreatedAt()
            )).collect(Collectors.toList());

            Object policySnapshotObj = parseJson(line.getPolicySnapshot());
            Object calcSnapshotObj = parseJson(line.getCalculationSnapshot());

            return new PayrollLineDto(
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
                    line.getPaidAmount(),
                    policySnapshotObj,
                    calcSnapshotObj,
                    adjDtos
            );
        }).collect(Collectors.toList());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
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
