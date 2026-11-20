package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.StockDocumentStatus;
import com.mtms.inventory.domain.StockDocumentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StockDocumentDto(
        UUID id,
        UUID branchId,
        String documentNumber,
        StockDocumentType documentType,
        LocalDate documentDate,
        StockDocumentStatus status,
        String reason,
        int totalLines,
        BigDecimal totalQuantity,
        BigDecimal totalBookedCost,
        UUID createdBy,
        String createdByName,
        UUID approvedBy,
        String approvedByName,
        Instant postedAt,
        Instant createdAt
) {}
