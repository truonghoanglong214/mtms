package com.mtms.closing.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-CLS-01..03 / BR-CLS-01, BR-CLS-02 / TC-016, TC-040, TC-041
 * Domain entity representing a monthly business close for a branch.
 */
public record MonthlyClose(
        UUID id,
        UUID branchId,
        LocalDate monthStart,
        int revision,
        boolean isCurrent,
        CloseStatus status,
        BigDecimal managerialProfit,
        boolean isProvisional,
        UUID closedBy,
        Instant closedAt,
        String snapshot,
        String note,
        Instant createdAt,
        Instant updatedAt,
        List<CloseComponent> components
) {
    public boolean isClosed() {
        return status == CloseStatus.CLOSED;
    }

    public static MonthlyClose createClosed(
            UUID branchId,
            LocalDate monthStart,
            BigDecimal managerialProfit,
            UUID closedBy,
            String snapshot,
            String note,
            List<CloseComponent> components
    ) {
        Instant now = Instant.now();
        return new MonthlyClose(
                UUID.randomUUID(),
                branchId,
                monthStart,
                1,
                true,
                CloseStatus.CLOSED,
                managerialProfit != null ? managerialProfit : BigDecimal.ZERO,
                true,
                closedBy,
                now,
                snapshot,
                note,
                now,
                now,
                components != null ? components : List.of()
        );
    }
}
