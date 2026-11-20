package com.mtms.revenue.application.service;

import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.revenue.application.dto.BusinessDayDto;
import com.mtms.revenue.domain.BusinessDay;
import com.mtms.revenue.domain.OperationStatus;
import com.mtms.revenue.domain.RevenueReportStatus;
import com.mtms.revenue.infrastructure.persistence.BusinessDayJpaEntity;
import com.mtms.revenue.infrastructure.persistence.BusinessDayJpaRepository;
import com.mtms.revenue.infrastructure.persistence.DailyRevenueReportJpaEntity;
import com.mtms.revenue.infrastructure.persistence.DailyRevenueReportJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * P0-REV-01 / BR-REV-02 / TC-004 / TC-005
 * Service managing branch business calendar status and missing revenue reports detection.
 */
@Service
public class BusinessCalendarService {

    private final BusinessDayJpaRepository businessDayRepository;
    private final DailyRevenueReportJpaRepository dailyRevenueReportRepository;
    private final BranchJpaRepository branchRepository;
    private final UserJpaRepository userRepository;

    public BusinessCalendarService(
            BusinessDayJpaRepository businessDayRepository,
            DailyRevenueReportJpaRepository dailyRevenueReportRepository,
            BranchJpaRepository branchRepository,
            UserJpaRepository userRepository
    ) {
        this.businessDayRepository = businessDayRepository;
        this.dailyRevenueReportRepository = dailyRevenueReportRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BusinessDayDto> getBusinessDays(UUID branchId, LocalDate startDate, LocalDate endDate) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

        List<BusinessDayJpaEntity> entities = businessDayRepository
                .findByBranchIdAndBusinessDateBetweenOrderByBusinessDateAsc(branchId, effectiveStart, effectiveEnd);

        Set<UUID> userIds = entities.stream()
                .map(BusinessDayJpaEntity::getRecordedBy)
                .collect(Collectors.toSet());
        Map<UUID, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName, (a, b) -> a));

        return entities.stream()
                .map(e -> toDto(e, userNames.get(e.getRecordedBy())))
                .collect(Collectors.toList());
    }

    @Transactional
    public BusinessDayDto setBusinessDay(
            UUID branchId,
            LocalDate businessDate,
            OperationStatus status,
            String closeReason,
            UUID actorUserId
    ) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        Optional<BusinessDayJpaEntity> existingOpt = businessDayRepository.findByBranchIdAndBusinessDate(branchId, businessDate);
        BusinessDay domain;
        BusinessDayJpaEntity saved;

        if (existingOpt.isPresent()) {
            BusinessDayJpaEntity entity = existingOpt.get();
            domain = entity.toDomain();
            domain.updateStatus(status, closeReason, actorUserId);
            entity.updateFromDomain(domain);
            saved = businessDayRepository.save(entity);
        } else {
            domain = BusinessDay.create(
                    branch.getBusinessEntityId(),
                    branchId,
                    businessDate,
                    status,
                    closeReason,
                    actorUserId
            );
            saved = businessDayRepository.save(BusinessDayJpaEntity.fromDomain(domain));
        }

        String recordedByName = userRepository.findById(actorUserId)
                .map(UserJpaEntity::getDisplayName)
                .orElse(null);

        return toDto(saved, recordedByName);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getMissingBusinessDates(UUID branchId, LocalDate startDate, LocalDate endDate) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found: " + branchId);
        }

        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

        if (effectiveStart.isAfter(effectiveEnd)) {
            LocalDate temp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = temp;
        }

        // Fetch configured calendar days in the range
        List<BusinessDayJpaEntity> calendarDays = businessDayRepository
                .findByBranchIdAndBusinessDateBetweenOrderByBusinessDateAsc(branchId, effectiveStart, effectiveEnd);

        Map<LocalDate, BusinessDayJpaEntity> calendarMap = calendarDays.stream()
                .collect(Collectors.toMap(BusinessDayJpaEntity::getBusinessDate, Function.identity(), (a, b) -> a));

        // Generate all dates in range
        List<LocalDate> allDates = new ArrayList<>();
        LocalDate cur = effectiveStart;
        while (!cur.isAfter(effectiveEnd)) {
            allDates.add(cur);
            cur = cur.plusDays(1);
        }

        // Fetch existing current reports in date range
        List<DailyRevenueReportJpaEntity> currentReports = dailyRevenueReportRepository
                .findByBranchIdAndBusinessDateInAndIsCurrentTrue(branchId, allDates);

        Map<LocalDate, DailyRevenueReportJpaEntity> reportMap = currentReports.stream()
                .collect(Collectors.toMap(DailyRevenueReportJpaEntity::getBusinessDate, Function.identity(), (a, b) -> a));

        List<LocalDate> missingDates = new ArrayList<>();
        for (LocalDate date : allDates) {
            BusinessDayJpaEntity cal = calendarMap.get(date);
            // TC-005: CLOSED day is strictly exempt from reporting
            if (cal != null && cal.getOperationStatus() == OperationStatus.CLOSED) {
                continue;
            }

            // TC-004: OPEN day requires CONFIRMED report
            DailyRevenueReportJpaEntity report = reportMap.get(date);
            if (report == null || report.getStatus() != RevenueReportStatus.CONFIRMED) {
                missingDates.add(date);
            }
        }

        return missingDates;
    }

    private BusinessDayDto toDto(BusinessDayJpaEntity entity, String recordedByName) {
        return new BusinessDayDto(
                entity.getId(),
                entity.getBranchId(),
                entity.getBusinessDate(),
                entity.getOperationStatus(),
                entity.getCloseReason(),
                recordedByName,
                entity.getCreatedAt()
        );
    }
}
