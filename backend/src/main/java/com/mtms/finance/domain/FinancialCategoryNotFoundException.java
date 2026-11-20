package com.mtms.finance.domain;

import java.util.UUID;

/**
 * P0-FIN-01
 * Thrown when a financial category is not found.
 */
public class FinancialCategoryNotFoundException extends RuntimeException {
    public FinancialCategoryNotFoundException(String message) {
        super(message);
    }

    public FinancialCategoryNotFoundException(UUID categoryId) {
        super("Không tìm thấy danh mục thu chi với ID: " + categoryId);
    }
}
