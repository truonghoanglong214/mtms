package com.mtms.inventory.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CommitOpeningCommand(
        UUID importBatchId,

        @NotNull(message = "Opening date is required")
        LocalDate openingDate,

        List<OpeningItemRow> rows,

        String note
) {
}
