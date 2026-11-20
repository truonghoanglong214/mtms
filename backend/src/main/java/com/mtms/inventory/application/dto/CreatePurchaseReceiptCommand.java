package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseReceiptCommand(
        UUID supplierId,
        @NotNull(message = "receivedOn must not be null")
        LocalDate receivedOn,
        String invoiceNumber,
        PaymentStatus paymentStatus,
        LocalDate dueDate,
        String notes,
        Boolean autoPost,
        @NotEmpty(message = "lines must not be empty")
        @Valid
        List<CreatePurchaseReceiptLineCommand> lines
) {
}
