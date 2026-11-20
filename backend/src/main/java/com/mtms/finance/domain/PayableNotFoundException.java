package com.mtms.finance.domain;

import java.util.UUID;

/**
 * P0-FIN-05
 * Thrown when payable debt record is not found.
 */
public class PayableNotFoundException extends RuntimeException {

    public PayableNotFoundException(String message) {
        super(message);
    }

    public PayableNotFoundException(UUID id) {
        super("Không tìm thấy khoản nợ phải trả với ID: " + id);
    }
}
