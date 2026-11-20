package com.mtms.finance.application.dto;

import com.mtms.finance.domain.TaxObligationStatus;
import com.mtms.finance.domain.TaxObligationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * Detailed DTO for a tax manual obligation including payment history.
 */
public record TaxObligationDetailDto(
        UUID id,
        UUID businessEntityId,
        UUID branchId,
        String obligationCode,
        TaxObligationType obligationType,
        String taxAuthorityName,
        LocalDate taxPeriodStart,
        LocalDate taxPeriodEnd,
        LocalDate dueDate,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal amountAllocated,
        BigDecimal remainingLiability,
        TaxObligationStatus status,
        String note,
        UUID evidenceFileId,
        UUID enteredBy,
        Instant createdAt,
        List<TaxPaymentDto> payments
) {}
