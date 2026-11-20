package com.mtms.stockcount.domain;

import java.math.BigDecimal;
import java.util.UUID;

public final class StockCountExceptions {

    private StockCountExceptions() {}

    public static class StockCountNotFoundException extends RuntimeException {
        public StockCountNotFoundException(UUID countId) {
            super("Không tìm thấy đợt kiểm kê với mã: " + countId);
        }
    }

    public static class InvalidStockCountStatusException extends RuntimeException {
        public InvalidStockCountStatusException(String message) {
            super(message);
        }
    }

    public static class MissingDiscrepancyReasonException extends RuntimeException {
        public MissingDiscrepancyReasonException(String itemName, BigDecimal varianceQty) {
            super("Mặt hàng '" + itemName + "' có chênh lệch (" + varianceQty + ") bắt buộc phải có lý do giải trình cụ thể.");
        }
    }

    public static class StockCountInsufficientStockException extends RuntimeException {
        public StockCountInsufficientStockException(String itemName, BigDecimal currentQty, BigDecimal shortageQty) {
            super("Tồn kho hiện tại (" + currentQty + ") không đủ để trừ lượng thiếu kiểm kê (" + shortageQty + ") cho mặt hàng: " + itemName + " (TC-021).");
        }
    }

    public static class StockCountImmutableException extends RuntimeException {
        public StockCountImmutableException(String message) {
            super(message);
        }
    }
}
