package com.mtms.finance.domain;

import java.util.UUID;

/**
 * P0-FIN-06 / TC-012
 * Thrown when attempting to post adjustment for a reconciliation with zero variance.
 */
public class NoVarianceToAdjustException extends RuntimeException {

    public NoVarianceToAdjustException(UUID id) {
        super("Biên bản đối soát " + id + " không có chênh lệch để thực hiện điều chỉnh");
    }

    public NoVarianceToAdjustException(String message) {
        super(message);
    }
}
