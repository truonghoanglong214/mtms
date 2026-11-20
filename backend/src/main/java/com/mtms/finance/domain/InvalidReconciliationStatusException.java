package com.mtms.finance.domain;

import java.util.UUID;

/**
 * P0-FIN-06 / TC-012
 * Thrown when attempting an invalid status transition on a reconciliation record.
 */
public class InvalidReconciliationStatusException extends RuntimeException {

    public InvalidReconciliationStatusException(UUID id, ReconciliationDisposition currentStatus, String targetAction) {
        super(String.format("Không thể thực hiện %s trên biên bản đối soát %s đang ở trạng thái %s",
                targetAction, id, currentStatus));
    }

    public InvalidReconciliationStatusException(String message) {
        super(message);
    }
}
