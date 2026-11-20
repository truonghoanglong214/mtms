package com.mtms.finance.domain;

import java.util.UUID;

/**
 * P0-FIN-05
 * Thrown when receivable record is not found.
 */
public class ReceivableNotFoundException extends RuntimeException {

    public ReceivableNotFoundException(String message) {
        super(message);
    }

    public ReceivableNotFoundException(UUID id) {
        super("Không tìm thấy khoản phải thu với ID: " + id);
    }
}
