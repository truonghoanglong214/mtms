package com.mtms.closing.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * P0-CLS-03 / BR-CLS-02 / TC-041
 * Domain model representing a detailed component breakdown of a closed month.
 */
public record CloseComponent(
        UUID id,
        UUID monthlyCloseId,
        CloseComponentCode componentCode,
        BigDecimal signedAmount,
        String provenance,
        Instant createdAt
) {
    public static CloseComponent create(
            UUID monthlyCloseId,
            CloseComponentCode componentCode,
            BigDecimal signedAmount,
            String provenance
    ) {
        return new CloseComponent(
                UUID.randomUUID(),
                monthlyCloseId,
                componentCode,
                signedAmount != null ? signedAmount : BigDecimal.ZERO,
                provenance != null ? provenance : "{}",
                Instant.now()
        );
    }
}
