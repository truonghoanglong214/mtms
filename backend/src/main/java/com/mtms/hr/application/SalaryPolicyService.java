package com.mtms.hr.application;

import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.common.idempotency.CurrentActorProvider;
import com.mtms.hr.application.dto.HrDtos.CreateSalaryPolicyCommand;
import com.mtms.hr.application.dto.HrDtos.SalaryPolicyDto;
import com.mtms.hr.domain.HrExceptions.EmployeeNotFoundException;
import com.mtms.hr.domain.HrExceptions.SalaryPolicyNotFoundException;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaEntity;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-HR-02 / BR-HR-01 / TC-013
 * Application service for managing effective wage and salary policies with midmonth slicing.
 */
@Service
@Transactional
public class SalaryPolicyService {

    private final SalaryPolicyJpaRepository salaryPolicyRepository;
    private final EmployeeJpaRepository employeeRepository;
    private final BranchJpaRepository branchRepository;
    private final CurrentActorProvider currentActorProvider;

    public SalaryPolicyService(
            SalaryPolicyJpaRepository salaryPolicyRepository,
            EmployeeJpaRepository employeeRepository,
            BranchJpaRepository branchRepository,
            CurrentActorProvider currentActorProvider
    ) {
        this.salaryPolicyRepository = salaryPolicyRepository;
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.currentActorProvider = currentActorProvider;
    }

    public SalaryPolicyDto createSalaryPolicy(
            UUID branchId,
            UUID employeeId,
            CreateSalaryPolicyCommand command
    ) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Không tìm thấy nhân viên: " + employeeId);
        }

        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        EmployeeService.validateSalaryPolicy(
                command.rateAmount(),
                command.standardWorkdays(),
                command.paidMinutesRoundingBlock(),
                command.effectiveFrom(),
                command.effectiveTo()
        );

        // TC-013 Invariant: When creating a new policy with an effective date,
        // automatically close previous overlapping policy/policies so history remains immutable.
        List<SalaryPolicyJpaEntity> previousPolicies =
                salaryPolicyRepository.findPoliciesToClose(employeeId, branchId, command.effectiveFrom());

        for (SalaryPolicyJpaEntity prev : previousPolicies) {
            if (prev.getEffectiveFrom().isBefore(command.effectiveFrom())) {
                prev.setEffectiveTo(command.effectiveFrom().minusDays(1));
                salaryPolicyRepository.save(prev);
            }
        }

        BigDecimal stdWorkdays = command.standardWorkdays() != null ? command.standardWorkdays() : BigDecimal.valueOf(26.00);
        Integer roundingBlock = command.paidMinutesRoundingBlock() != null ? command.paidMinutesRoundingBlock() : 15;

        SalaryPolicyJpaEntity newPolicy = new SalaryPolicyJpaEntity(
                UUID.randomUUID(),
                employeeId,
                branchId,
                command.rateType(),
                command.rateAmount(),
                stdWorkdays,
                roundingBlock,
                command.effectiveFrom(),
                command.effectiveTo(),
                command.notes(),
                currentActorProvider != null ? currentActorProvider.getCurrentActorId() : null,
                Instant.now()
        );
        salaryPolicyRepository.save(newPolicy);

        return new SalaryPolicyDto(
                newPolicy.getId(),
                newPolicy.getEmployeeId(),
                newPolicy.getBranchId(),
                newPolicy.getRateType(),
                newPolicy.getRateAmount(),
                newPolicy.getStandardWorkdays(),
                newPolicy.getPaidMinutesRoundingBlock(),
                newPolicy.getEffectiveFrom(),
                newPolicy.getEffectiveTo(),
                newPolicy.getNotes(),
                newPolicy.getApprovedBy(),
                newPolicy.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public SalaryPolicyDto getEffectiveSalaryPolicy(
            UUID branchId,
            UUID employeeId,
            LocalDate date
    ) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Không tìm thấy nhân viên: " + employeeId);
        }

        List<SalaryPolicyJpaEntity> policies =
                salaryPolicyRepository.findEffectivePolicies(employeeId, branchId, date);

        if (policies.isEmpty()) {
            throw new SalaryPolicyNotFoundException(
                    "Không tìm thấy chính sách lương hiệu lực cho nhân viên " + employeeId + " tại chi nhánh " + branchId + " vào ngày " + date
            );
        }

        SalaryPolicyJpaEntity policy = policies.get(0);
        return new SalaryPolicyDto(
                policy.getId(),
                policy.getEmployeeId(),
                policy.getBranchId(),
                policy.getRateType(),
                policy.getRateAmount(),
                policy.getStandardWorkdays(),
                policy.getPaidMinutesRoundingBlock(),
                policy.getEffectiveFrom(),
                policy.getEffectiveTo(),
                policy.getNotes(),
                policy.getApprovedBy(),
                policy.getCreatedAt()
        );
    }
}
