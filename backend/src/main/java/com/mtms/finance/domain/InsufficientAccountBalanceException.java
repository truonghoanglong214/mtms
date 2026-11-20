package com.mtms.finance.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * P0-FIN-02 / BR-FIN-09
 * Thrown when an outflow operation exceeds the available current balance of an account.
 */
public class InsufficientAccountBalanceException extends RuntimeException {

    public InsufficientAccountBalanceException(String message) {
        super(message);
    }

    public InsufficientAccountBalanceException(UUID accountId, BigDecimal requested, BigDecimal available) {
        super(String.format("Tài khoản %s không đủ số dư để thực hiện giao dịch (Yêu cầu: %s, Khả dụng: %s)",
                accountId, requested, available));
    }
}
