package com.mtms.finance.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * Command to explain a reconciliation variance/discrepancy.
 */
public record ExplainDiscrepancyCommand(
        @NotBlank(message = "Lý do giải trình chênh lệch không được để trống")
        String note,

        UUID evidenceFileId
) {}
