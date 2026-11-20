package com.mtms.revenue.application.dto;

import com.mtms.revenue.domain.OperationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessDayDto(
        UUID id,
        UUID branchId,
        LocalDate businessDate,
        OperationStatus operationStatus,
        String closeReason,
        String recordedByName,
        Instant createdAt
) {
}
