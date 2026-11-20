package com.mtms.finance.domain;

/**
 * P0-FIN-02 / TC-010
 * Thrown when an internal transfer is attempted between identical accounts or accounts from different branches.
 */
public class InvalidTransferAccountsException extends RuntimeException {
    public InvalidTransferAccountsException(String message) {
        super(message);
    }
}
