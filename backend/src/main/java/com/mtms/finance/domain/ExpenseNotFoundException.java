package com.mtms.finance.domain;

import java.util.UUID;

/**
 * Thrown when an expense record is not found for a branch.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(UUID id) {
        super("Không tìm thấy phiếu chi: " + id);
    }

    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
