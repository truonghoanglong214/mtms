package com.mtms.finance.domain;

import java.util.UUID;

/**
 * P0-FIN-06 / TC-012
 * Thrown when a cash reconciliation record is not found for a branch.
 */
public class ReconciliationNotFoundException extends RuntimeException {

    public ReconciliationNotFoundException(UUID id) {
        super("Không tìm thấy biên bản đối soát tài khoản: " + id);
    }

    public ReconciliationNotFoundException(String message) {
        super(message);
    }
}
