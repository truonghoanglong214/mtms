package com.mtms.finance.domain;

import java.util.UUID;

/**
 * P0-FIN-06 / TC-012
 * Thrown when attempting to adjust an already adjusted or closed reconciliation.
 */
public class ReconciliationAlreadyAdjustedException extends RuntimeException {

    public ReconciliationAlreadyAdjustedException(UUID id) {
        super("Biên bản đối soát " + id + " đã được điều chỉnh hoặc đã đóng");
    }

    public ReconciliationAlreadyAdjustedException(String message) {
        super(message);
    }
}
