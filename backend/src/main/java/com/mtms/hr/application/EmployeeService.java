package com.mtms.hr.application;

import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.common.idempotency.CurrentActorProvider;
import com.mtms.hr.application.dto.HrDtos.CreateAssignmentCommand;
import com.mtms.hr.application.dto.HrDtos.CreateEmployeeCommand;
import com.mtms.hr.application.dto.HrDtos.CreatedEmployeeResponse;
import com.mtms.hr.application.dto.HrDtos.EmployeeAssignmentDto;
import com.mtms.hr.application.dto.HrDtos.EmployeeDetailDto;
import com.mtms.hr.application.dto.HrDtos.EmployeeSummaryDto;
import com.mtms.hr.application.dto.HrDtos.PagedResponse;
import com.mtms.hr.application.dto.HrDtos.SalaryPolicyDto;
import com.mtms.hr.application.dto.HrDtos.UpdateEmployeeCommand;
import com.mtms.hr.domain.EmployeeStatus;
import com.mtms.hr.domain.HrExceptions.EmployeeNotFoundException;
import com.mtms.hr.domain.HrExceptions.InvalidEmployeeDatesException;
import com.mtms.hr.domain.HrExceptions.InvalidSalaryPolicyException;
import com.mtms.hr.domain.HrExceptions.StaffCodeConflictException;
import com.mtms.hr.domain.RateType;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaEntity;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-HR-01 / BR-HR-03 / TC-015
 * Application service for workforce employee lifecycle, branch allocations, and directory queries.
 */
@Service
@Transactional
public class EmployeeService {

    private static final Set<Integer> VALID_ROUNDING_BLOCKS = Set.of(1, 5, 10, 15, 30, 60);

    private final EmployeeJpaRepository employeeRepository;
    private final EmployeeAssignmentJpaRepository assignmentRepository;
    private final SalaryPolicyJpaRepository salaryPolicyRepository;
    private final BranchJpaRepository branchRepository;
    private final CurrentActorProvider currentActorProvider;

    public EmployeeService(
            EmployeeJpaRepository employeeRepository,
            EmployeeAssignmentJpaRepository assignmentRepository,
            SalaryPolicyJpaRepository salaryPolicyRepository,
            BranchJpaRepository branchRepository,
            CurrentActorProvider currentActorProvider
    ) {
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.salaryPolicyRepository = salaryPolicyRepository;
        this.branchRepository = branchRepository;
        this.currentActorProvider = currentActorProvider;
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeSummaryDto> getEmployees(
            UUID branchId,
            String search,
            EmployeeStatus status,
            Pageable pageable
    ) {
        Page<EmployeeJpaEntity> employeePage = employeeRepository.findByBranch(
                branchId,
                status,
                search != null ? search.trim() : null,
                pageable
        );

        LocalDate today = LocalDate.now();

        Page<EmployeeSummaryDto> summaryPage = employeePage.map(employee -> {
            List<EmployeeAssignmentJpaEntity> branchAssignments =
                    assignmentRepository.findByEmployeeIdAndBranchId(employee.getId(), branchId);

            boolean isPrimary = branchAssignments.stream().anyMatch(a ->
                    a.isPrimary()
                            && !a.getEffectiveFrom().isAfter(today)
                            && (a.getEffectiveTo() == null || !a.getEffectiveTo().isBefore(today))
            );

            if (!isPrimary && !branchAssignments.isEmpty()) {
                isPrimary = branchAssignments.stream()
                        .max(Comparator.comparing(EmployeeAssignmentJpaEntity::getEffectiveFrom))
                        .map(EmployeeAssignmentJpaEntity::isPrimary)
                        .orElse(false);
            }

            List<SalaryPolicyJpaEntity> effectivePolicies =
                    salaryPolicyRepository.findEffectivePolicies(employee.getId(), branchId, today);

            RateType rateType = null;
            BigDecimal rateAmount = null;
            if (!effectivePolicies.isEmpty()) {
                rateType = effectivePolicies.get(0).getRateType();
                rateAmount = effectivePolicies.get(0).getRateAmount();
            }

            return new EmployeeSummaryDto(
                    employee.getId(),
                    employee.getStaffCode(),
                    employee.getFullName(),
                    employee.getPositionName(),
                    employee.getPhoneNumber(),
                    employee.getEmail(),
                    employee.getHiredOn(),
                    employee.getLeftOn(),
                    employee.getStatus(),
                    isPrimary,
                    rateType,
                    rateAmount,
                    employee.getCreatedAt()
            );
        });

        return PagedResponse.from(summaryPage);
    }

    public CreatedEmployeeResponse createEmployee(UUID branchId, CreateEmployeeCommand command) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        if (employeeRepository.existsByStaffCode(command.staffCode().trim())) {
            throw new StaffCodeConflictException("Mã nhân viên đã tồn tại trong hệ thống: " + command.staffCode().trim());
        }

        if (command.leftOn() != null && command.leftOn().isBefore(command.hiredOn())) {
            throw new InvalidEmployeeDatesException("Ngày thôi việc không được trước ngày tuyển dụng: " + command.leftOn() + " < " + command.hiredOn());
        }

        if (command.initialSalaryPolicy() != null) {
            validateSalaryPolicy(
                    command.initialSalaryPolicy().rateAmount(),
                    command.initialSalaryPolicy().standardWorkdays(),
                    command.initialSalaryPolicy().paidMinutesRoundingBlock(),
                    command.initialSalaryPolicy().effectiveFrom() != null ? command.initialSalaryPolicy().effectiveFrom() : command.hiredOn(),
                    null
            );
        }

        Instant now = Instant.now();
        UUID employeeId = UUID.randomUUID();

        EmployeeJpaEntity employee = new EmployeeJpaEntity(
                employeeId,
                null,
                command.staffCode().trim(),
                command.fullName().trim(),
                command.positionName().trim(),
                command.phoneNumber() != null ? command.phoneNumber().trim() : null,
                command.email() != null ? command.email().trim() : null,
                command.hiredOn(),
                command.leftOn(),
                EmployeeStatus.ACTIVE,
                command.notes(),
                now,
                now
        );
        employeeRepository.save(employee);

        // Auto-assign to current branch as primary branch (TC-015)
        EmployeeAssignmentJpaEntity initialAssignment = new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(),
                employeeId,
                branchId,
                true,
                command.hiredOn(),
                null,
                "Phân bổ chi nhánh ban đầu",
                now
        );
        assignmentRepository.save(initialAssignment);

        // Optional initial salary policy setup (TC-013)
        if (command.initialSalaryPolicy() != null) {
            var policyCmd = command.initialSalaryPolicy();
            LocalDate effFrom = policyCmd.effectiveFrom() != null ? policyCmd.effectiveFrom() : command.hiredOn();
            BigDecimal stdWorkdays = policyCmd.standardWorkdays() != null ? policyCmd.standardWorkdays() : BigDecimal.valueOf(26.00);
            Integer roundingBlock = policyCmd.paidMinutesRoundingBlock() != null ? policyCmd.paidMinutesRoundingBlock() : 15;

            SalaryPolicyJpaEntity initialPolicy = new SalaryPolicyJpaEntity(
                    UUID.randomUUID(),
                    employeeId,
                    branchId,
                    policyCmd.rateType(),
                    policyCmd.rateAmount(),
                    stdWorkdays,
                    roundingBlock,
                    effFrom,
                    null,
                    policyCmd.notes() != null ? policyCmd.notes() : "Chính sách lương ban đầu",
                    currentActorProvider != null ? currentActorProvider.getCurrentActorId() : null,
                    now
            );
            salaryPolicyRepository.save(initialPolicy);
        }

        return new CreatedEmployeeResponse(
                employee.getId(),
                employee.getStaffCode(),
                employee.getFullName(),
                employee.getPositionName(),
                employee.getPhoneNumber(),
                employee.getEmail(),
                employee.getHiredOn(),
                employee.getLeftOn(),
                employee.getStatus(),
                employee.getNotes(),
                employee.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public EmployeeDetailDto getEmployeeDetail(UUID branchId, UUID employeeId) {
        EmployeeJpaEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + employeeId));

        List<EmployeeAssignmentJpaEntity> assignments =
                assignmentRepository.findByEmployeeIdOrderByEffectiveFromAscCreatedAtAsc(employeeId);

        Map<UUID, String> branchNames = branchRepository.findAllById(
                assignments.stream().map(EmployeeAssignmentJpaEntity::getBranchId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(BranchJpaEntity::getId, BranchJpaEntity::getName));

        List<EmployeeAssignmentDto> assignmentDtos = assignments.stream()
                .map(a -> new EmployeeAssignmentDto(
                        a.getId(),
                        a.getBranchId(),
                        branchNames.getOrDefault(a.getBranchId(), "Unknown Branch"),
                        a.isPrimary(),
                        a.getEffectiveFrom(),
                        a.getEffectiveTo(),
                        a.getNotes(),
                        a.getCreatedAt()
                ))
                .toList();

        List<SalaryPolicyJpaEntity> policies =
                salaryPolicyRepository.findByEmployeeIdOrderByEffectiveFromAscCreatedAtAsc(employeeId);

        List<SalaryPolicyDto> policyDtos = policies.stream()
                .map(p -> new SalaryPolicyDto(
                        p.getId(),
                        p.getEmployeeId(),
                        p.getBranchId(),
                        p.getRateType(),
                        p.getRateAmount(),
                        p.getStandardWorkdays(),
                        p.getPaidMinutesRoundingBlock(),
                        p.getEffectiveFrom(),
                        p.getEffectiveTo(),
                        p.getNotes(),
                        p.getApprovedBy(),
                        p.getCreatedAt()
                ))
                .toList();

        return new EmployeeDetailDto(
                employee.getId(),
                employee.getStaffCode(),
                employee.getFullName(),
                employee.getPositionName(),
                employee.getPhoneNumber(),
                employee.getEmail(),
                employee.getHiredOn(),
                employee.getLeftOn(),
                employee.getStatus(),
                employee.getNotes(),
                assignmentDtos,
                policyDtos
        );
    }

    public EmployeeDetailDto updateEmployee(UUID branchId, UUID employeeId, UpdateEmployeeCommand command) {
        EmployeeJpaEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + employeeId));

        if (command.leftOn() != null && command.leftOn().isBefore(employee.getHiredOn())) {
            throw new InvalidEmployeeDatesException("Ngày thôi việc không được trước ngày tuyển dụng: " + command.leftOn() + " < " + employee.getHiredOn());
        }

        employee.setFullName(command.fullName().trim());
        employee.setPositionName(command.positionName().trim());
        employee.setPhoneNumber(command.phoneNumber() != null ? command.phoneNumber().trim() : null);
        employee.setEmail(command.email() != null ? command.email().trim() : null);
        if (command.status() != null) {
            employee.setStatus(command.status());
        }
        employee.setLeftOn(command.leftOn());
        employee.setNotes(command.notes());
        employee.setUpdatedAt(Instant.now());

        employeeRepository.save(employee);
        return getEmployeeDetail(branchId, employeeId);
    }

    public EmployeeAssignmentDto assignBranch(UUID currentBranchId, UUID employeeId, CreateAssignmentCommand command) {
        EmployeeJpaEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + employeeId));

        BranchJpaEntity targetBranch = branchRepository.findById(command.targetBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Target branch not found: " + command.targetBranchId()));

        if (command.effectiveTo() != null && command.effectiveTo().isBefore(command.effectiveFrom())) {
            throw new InvalidEmployeeDatesException("effectiveTo cannot be before effectiveFrom: " + command.effectiveTo() + " < " + command.effectiveFrom());
        }

        boolean isPrimary = Boolean.TRUE.equals(command.isPrimary());

        if (isPrimary) {
            // TC-015 Primary Branch Allocation Invariant:
            // At any point in time, employee can have at most ONE primary branch.
            // Automatically close previous primary assignment(s) up to effectiveFrom.minusDays(1).
            List<EmployeeAssignmentJpaEntity> conflicts =
                    assignmentRepository.findConflictingPrimaryAssignments(employeeId, command.effectiveFrom());

            for (EmployeeAssignmentJpaEntity prev : conflicts) {
                if (prev.getEffectiveFrom().isBefore(command.effectiveFrom())) {
                    prev.setEffectiveTo(command.effectiveFrom().minusDays(1));
                    assignmentRepository.save(prev);
                } else {
                    prev.setPrimary(false);
                    assignmentRepository.save(prev);
                }
            }
        }

        EmployeeAssignmentJpaEntity newAssignment = new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(),
                employeeId,
                command.targetBranchId(),
                isPrimary,
                command.effectiveFrom(),
                command.effectiveTo(),
                command.notes(),
                Instant.now()
        );
        assignmentRepository.save(newAssignment);

        return new EmployeeAssignmentDto(
                newAssignment.getId(),
                newAssignment.getBranchId(),
                targetBranch.getName(),
                newAssignment.isPrimary(),
                newAssignment.getEffectiveFrom(),
                newAssignment.getEffectiveTo(),
                newAssignment.getNotes(),
                newAssignment.getCreatedAt()
        );
    }

    public static void validateSalaryPolicy(
            BigDecimal rateAmount,
            BigDecimal standardWorkdays,
            Integer paidMinutesRoundingBlock,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        if (rateAmount == null || rateAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidSalaryPolicyException("Đơn giá lương không được âm: " + rateAmount);
        }
        if (standardWorkdays != null && standardWorkdays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidSalaryPolicyException("Số ngày công chuẩn phải lớn hơn 0: " + standardWorkdays);
        }
        if (paidMinutesRoundingBlock != null && !VALID_ROUNDING_BLOCKS.contains(paidMinutesRoundingBlock)) {
            throw new InvalidSalaryPolicyException("Khối làm tròn phút tính công phải thuộc {1, 5, 10, 15, 30, 60}: " + paidMinutesRoundingBlock);
        }
        if (effectiveFrom == null) {
            throw new InvalidSalaryPolicyException("Ngày hiệu lực không được để trống");
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new InvalidSalaryPolicyException("Ngày kết thúc hiệu lực không được trước ngày bắt đầu: " + effectiveTo + " < " + effectiveFrom);
        }
    }
}
