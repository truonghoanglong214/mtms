package com.mtms.finance.domain;

/**
 * P0-FIN-02 / TC-011
 * Thrown when a duplicate ledger posting is detected for the same account, source_type, source_id, and source_leg.
 */
public class DuplicateLedgerPostingException extends RuntimeException {

    public DuplicateLedgerPostingException(String message) {
        super(message);
    }

    public DuplicateLedgerPostingException(String message, Throwable cause) {
        super(message, cause);
    }
}
