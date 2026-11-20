package com.mtms.revenue.application.service;

import com.mtms.common.decimal.MoneyUtils;
import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.finance.application.RevenueCashLinkageService;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.revenue.application.dto.ConfirmRevenueCommand;
import com.mtms.revenue.application.dto.CreateDailyRevenueCommand;
import com.mtms.revenue.application.dto.DailyRevenueListResponse;
import com.mtms.revenue.application.dto.DailyRevenueReportDto;
import com.mtms.revenue.domain.DailyReportNotFoundException;
import com.mtms.revenue.domain.DailyRevenueReport;
import com.mtms.revenue.domain.DuplicateDailyReportException;
import com.mtms.revenue.domain.ReportAlreadyConfirmedException;
import com.mtms.revenue.domain.RevenueReportStatus;
import com.mtms.revenue.infrastructure.persistence.DailyRevenueReportJpaEntity;
import com.mtms.revenue.infrastructure.persistence.DailyRevenueReportJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * P0-REV-02 / P0-REV-03 / BR-REV-01 / BR-REV-03 / TC-006 / P0-REV-05 / BR-REV-04
 * Application service for daily revenue report creation, confirmation, adjustments, revision management,
 * and automatic revenue cash/bank ledger linkage.
 */
@Service
public class DailyRevenueService {

    private final DailyRevenueReportJpaRepository dailyRevenueReportRepository;
    private final BranchJpaRepository branchRepository;
    private final UserJpaRepository userRepository;
    private final RevenueCashLinkageService revenueCashLinkageService;

    public DailyRevenueService(
            DailyRevenueReportJpaRepository dailyRevenueReportRepository,
            BranchJpaRepository branchRepository,
            UserJpaRepository userRepository,
            RevenueCashLinkageService revenueCashLinkageService
    ) {
        this.dailyRevenueReportRepository = dailyRevenueReportRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.revenueCashLinkageService = revenueCashLinkageService;
    }

    @Transactional
    public DailyRevenueReportDto createReport(
            UUID branchId,
            CreateDailyRevenueCommand command,
            UUID actorUserId
    ) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        // Check if an active/current report already exists for this branch and date
        Optional<DailyRevenueReportJpaEntity> existingCurrent = dailyRevenueReportRepository
                .findByBranchIdAndBusinessDateAndIsCurrentTrue(branchId, command.businessDate());

        if (existingCurrent.isPresent()) {
            throw new DuplicateDailyReportException(
                    "An active report already exists for branch " + branchId + " on date " + command.businessDate()
            );
        }

        boolean submitNow = Boolean.TRUE.equals(command.submitImmediately());

        DailyRevenueReport domain = DailyRevenueReport.create(
                branch.getBusinessEntityId(),
                branchId,
                command.businessDate(),
                command.cashAmount(),
                command.bankTransferAmount(),
                command.deliveryAppNetAmount(),
                command.otherAdjustmentAmount(),
                command.totalRevenue(),
                command.evidenceFileId(),
                command.note(),
                submitNow,
                actorUserId
        );

        DailyRevenueReportJpaEntity saved = dailyRevenueReportRepository
                .save(DailyRevenueReportJpaEntity.fromDomain(domain));

        String actorName = userRepository.findById(actorUserId)
                .map(UserJpaEntity::getDisplayName)
                .orElse(null);

        return toDto(saved, actorName, null);
    }

    @Transactional
    public DailyRevenueReportDto confirmReport(
            UUID branchId,
            UUID reportId,
            ConfirmRevenueCommand command,
            UUID actorUserId
    ) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        DailyRevenueReportJpaEntity entity = dailyRevenueReportRepository.findByIdAndBranchId(reportId, branchId)
                .orElseThrow(() -> new DailyReportNotFoundException("Daily revenue report not found: " + reportId));

        DailyRevenueReport domain = entity.toDomain();

        // Check if adjustments are provided
        boolean hasAdjustmentAmounts = command != null && (
                command.cashAmount() != null ||
                command.bankTransferAmount() != null ||
                command.deliveryAppNetAmount() != null ||
                command.otherAdjustmentAmount() != null ||
                command.totalRevenue() != null
        );

        if (domain.getStatus() == RevenueReportStatus.CONFIRMED) {
            // TC-006 Idempotency: If already CONFIRMED and no amount adjustments requested, return cleanly
            if (!hasAdjustmentAmounts) {
                return toDtoWithUserLookup(entity);
            }

            // If adjustments are requested on a CONFIRMED report, create a new revision
            DailyRevenueReport newRevision = domain.createAdjustmentRevision(
                    command.cashAmount(),
                    command.bankTransferAmount(),
                    command.deliveryAppNetAmount(),
                    command.otherAdjustmentAmount(),
                    command.totalRevenue(),
                    command.adjustmentReason(),
                    command.evidenceFileId(),
                    command.note(),
                    actorUserId
            );

            // Save old as superseded and save new
            entity.updateFromDomain(domain);
            dailyRevenueReportRepository.save(entity);
            DailyRevenueReportJpaEntity savedNew = dailyRevenueReportRepository
                    .save(DailyRevenueReportJpaEntity.fromDomain(newRevision));

            // Link new revision revenue and reverse superseded report's active entries
            revenueCashLinkageService.linkDailyRevenue(
                    branchId,
                    savedNew.getId(),
                    savedNew.getBusinessDate(),
                    savedNew.getCashAmount(),
                    savedNew.getBankTransferAmount(),
                    entity.getId(),
                    actorUserId
            );

            return toDtoWithUserLookup(savedNew);
        }

        // If report is DRAFT or SUBMITTED
        if (hasAdjustmentAmounts) {
            domain.setFinancialAmounts(
                    command.cashAmount() != null ? command.cashAmount() : domain.getCashAmount(),
                    command.bankTransferAmount() != null ? command.bankTransferAmount() : domain.getBankTransferAmount(),
                    command.deliveryAppNetAmount() != null ? command.deliveryAppNetAmount() : domain.getDeliveryAppNetAmount(),
                    command.otherAdjustmentAmount() != null ? command.otherAdjustmentAmount() : domain.getOtherAdjustmentAmount(),
                    command.totalRevenue()
            );
        }

        String reason = command != null ? command.adjustmentReason() : null;
        domain.confirm(actorUserId, reason);

        entity.updateFromDomain(domain);
        DailyRevenueReportJpaEntity saved = dailyRevenueReportRepository.save(entity);

        // Synchronously link confirmed daily revenue into cash and bank subledgers
        revenueCashLinkageService.linkDailyRevenue(
                branchId,
                saved.getId(),
                saved.getBusinessDate(),
                saved.getCashAmount(),
                saved.getBankTransferAmount(),
                null,
                actorUserId
        );

        return toDtoWithUserLookup(saved);
    }

    @Transactional
    public DailyRevenueReportDto updateReport(
            UUID branchId,
            UUID reportId,
            CreateDailyRevenueCommand command,
            UUID actorUserId
    ) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        DailyRevenueReportJpaEntity entity = dailyRevenueReportRepository.findByIdAndBranchId(reportId, branchId)
                .orElseThrow(() -> new DailyReportNotFoundException("Daily revenue report not found: " + reportId));

        DailyRevenueReport domain = entity.toDomain();

        if (domain.getStatus() == RevenueReportStatus.CONFIRMED) {
            throw new ReportAlreadyConfirmedException("Report is already confirmed. Adjustments must be confirmed as revisions.");
        }

        domain.setFinancialAmounts(
                command.cashAmount(),
                command.bankTransferAmount(),
                command.deliveryAppNetAmount(),
                command.otherAdjustmentAmount(),
                command.totalRevenue()
        );

        if (Boolean.TRUE.equals(command.submitImmediately())) {
            domain.submit(actorUserId);
        }

        entity.updateFromDomain(domain);
        DailyRevenueReportJpaEntity saved = dailyRevenueReportRepository.save(entity);

        return toDtoWithUserLookup(saved);
    }

    @Transactional(readOnly = true)
    public DailyRevenueReportDto getDailyRevenueReportById(UUID branchId, UUID reportId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        DailyRevenueReportJpaEntity entity = dailyRevenueReportRepository.findByIdAndBranchId(reportId, branchId)
                .orElseThrow(() -> new DailyReportNotFoundException("Daily revenue report not found: " + reportId));

        return toDtoWithUserLookup(entity);
    }

    @Transactional(readOnly = true)
    public DailyRevenueListResponse getDailyRevenueReports(
            UUID branchId,
            LocalDate startDate,
            LocalDate endDate,
            RevenueReportStatus status,
            Pageable pageable
    ) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        Page<DailyRevenueReportJpaEntity> page = dailyRevenueReportRepository.findCurrentReports(
                branchId,
                startDate,
                endDate,
                status,
                pageable
        );

        Set<UUID> userIds = page.getContent().stream()
                .flatMap(r -> Stream.of(r.getSubmittedBy(), r.getConfirmedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName, (a, b) -> a));

        List<DailyRevenueReportDto> dtos = page.getContent().stream()
                .map(r -> toDto(
                        r,
                        userNames.get(r.getSubmittedBy()),
                        r.getConfirmedBy() != null ? userNames.get(r.getConfirmedBy()) : null
                ))
                .collect(Collectors.toList());

        return new DailyRevenueListResponse(dtos, page.getTotalElements());
    }

    private DailyRevenueReportDto toDtoWithUserLookup(DailyRevenueReportJpaEntity entity) {
        String submittedByName = userRepository.findById(entity.getSubmittedBy())
                .map(UserJpaEntity::getDisplayName)
                .orElse(null);

        String confirmedByName = entity.getConfirmedBy() != null
                ? userRepository.findById(entity.getConfirmedBy()).map(UserJpaEntity::getDisplayName).orElse(null)
                : null;

        return toDto(entity, submittedByName, confirmedByName);
    }

    private DailyRevenueReportDto toDto(
            DailyRevenueReportJpaEntity entity,
            String submittedByName,
            String confirmedByName
    ) {
        return new DailyRevenueReportDto(
                entity.getId(),
                entity.getBranchId(),
                entity.getBusinessDate(),
                entity.getRevision(),
                entity.isCurrent(),
                formatAmount(entity.getCashAmount()),
                formatAmount(entity.getBankTransferAmount()),
                formatAmount(entity.getDeliveryAppNetAmount()),
                formatAmount(entity.getOtherAdjustmentAmount()),
                formatAmount(entity.getTotalRevenue()),
                entity.getStatus(),
                submittedByName,
                entity.getSubmittedAt(),
                confirmedByName,
                entity.getConfirmedAt(),
                entity.getAdjustmentReason(),
                entity.getEvidenceFileId(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
