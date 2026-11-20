package com.mtms.finance.domain;

import java.util.UUID;

/**
 * P0-FIN-02
 * Thrown when a financial account cannot be found.
 */
public class FinancialAccountNotFoundException extends RuntimeException {

    public FinancialAccountNotFoundException(String message) {
        super(message);
    }

    public FinancialAccountNotFoundException(UUID accountId) {
        super("Không tìm thấy tài khoản tài chính với ID: " + accountId);
    }
}
