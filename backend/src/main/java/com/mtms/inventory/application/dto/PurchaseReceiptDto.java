package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.PaymentStatus;
import com.mtms.inventory.domain.ReceiptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseReceiptDto(
        UUID id,
        UUID branchId,
        String receiptNumber,
        UUID supplierId,
        String supplierName,
        LocalDate receivedOn,
        String invoiceNumber,
        BigDecimal invoiceTotal,
        PaymentStatus paymentStatus,
        UUID payableId,
        ReceiptStatus status,
        int totalLines,
        String postedByName,
        Instant postedAt
) {
}
