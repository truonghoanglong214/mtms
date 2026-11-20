package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.StockDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateStockIssueCommand(
        @NotNull(message = "documentDate must not be null")
        LocalDate documentDate,

        StockDocumentType documentType,

        String reason,

        Boolean autoPost,

        @NotEmpty(message = "lines must not be empty")
        @Valid
        List<CreateStockIssueLineCommand> lines
) {
    public StockDocumentType getEffectiveDocumentType() {
        return documentType != null ? documentType : StockDocumentType.CONSUMPTION_ISSUE;
    }
}
