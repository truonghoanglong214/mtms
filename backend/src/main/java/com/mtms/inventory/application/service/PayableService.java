package com.mtms.inventory.application.service;

import com.mtms.inventory.application.dto.BranchPayablesResponse;
import com.mtms.inventory.application.dto.PayableDto;
import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.inventory.domain.Payable;
import com.mtms.inventory.domain.PayableRepository;
import com.mtms.inventory.domain.PayableStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PayableService {

    private final BranchJpaRepository branchRepository;
    private final PayableRepository payableRepository;

    public PayableService(BranchJpaRepository branchRepository, PayableRepository payableRepository) {
        this.branchRepository = branchRepository;
        this.payableRepository = payableRepository;
    }

    @Transactional(readOnly = true)
    public BranchPayablesResponse getBranchPayables(
            UUID branchId,
            PayableStatus status,
            String search,
            Pageable pageable
    ) {
        if (!branchRepository.existsById(branchId)) {
            throw new BranchNotFoundException(branchId);
        }

        Page<Payable> page = payableRepository.findByBranchIdWithFilters(branchId, status, search, pageable);
        BigDecimal totalDebt = payableRepository.calculateTotalOutstandingDebt(branchId);

        List<PayableDto> items = page.getContent().stream()
                .map(p -> new PayableDto(
                        p.getId(),
                        p.getBranchId(),
                        p.getCounterpartyLabel(),
                        p.getSourceType(),
                        p.getSourceId(),
                        p.getIncurredDate(),
                        p.getDueDate(),
                        p.getOriginalAmount(),
                        p.getPaidAmount(),
                        p.getRemainingAmount(),
                        p.getStatus(),
                        p.getCreatedAt()
                ))
                .toList();

        return new BranchPayablesResponse(items, page.getTotalElements(), totalDebt);
    }
}
