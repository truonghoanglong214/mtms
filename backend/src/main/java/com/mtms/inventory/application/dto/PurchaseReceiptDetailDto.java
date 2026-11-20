package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.PaymentStatus;
import com.mtms.inventory.domain.ReceiptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseReceiptDetailDto(
        UUID id,
        UUID branchId,
        UUID businessEntityId,
        String receiptNumber,
        UUID supplierId,
        String supplierName,
        LocalDate receivedOn,
        String invoiceNumber,
        BigDecimal invoiceTotal,
        PaymentStatus paymentStatus,
        UUID payableId,
        ReceiptStatus status,
        String notes,
        UUID postedBy,
        String postedByName,
        Instant postedAt,
        Instant createdAt,
        int totalLines,
        List<PurchaseReceiptLineDto> lines
) {
}
