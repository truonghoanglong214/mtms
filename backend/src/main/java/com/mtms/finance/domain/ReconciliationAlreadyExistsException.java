package com.mtms.finance.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-FIN-06 / TC-012
 * Thrown when a reconciliation record already exists for the given account and business date.
 */
public class ReconciliationAlreadyExistsException extends RuntimeException {

    public ReconciliationAlreadyExistsException(UUID accountId, LocalDate date) {
        super(String.format("Tài khoản %s đã có biên bản đối soát vào ngày %s", accountId, date));
    }

    public ReconciliationAlreadyExistsException(String message) {
        super(message);
    }
}
