package com.mtms.closing.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * P0-CLS-01..04 Domain Exceptions
 */
public final class ClosingExceptions {

    private ClosingExceptions() {}

    public static class ClosingPrerequisiteFailedException extends RuntimeException {
        public ClosingPrerequisiteFailedException(String message) {
            super(message);
        }
    }

    public static class MonthAlreadyClosedException extends RuntimeException {
        public MonthAlreadyClosedException(UUID branchId, LocalDate monthStart) {
            super("Kỳ kinh doanh tháng " + monthStart + " của chi nhánh đã được chốt sổ trước đó.");
        }
    }

    public static class ClosedPeriodLockedException extends RuntimeException {
        public ClosedPeriodLockedException(UUID branchId, LocalDate monthStart) {
            super("Kỳ kinh doanh tháng " + monthStart + " đã chốt sổ (CLOSED). Cấm sửa đổi số liệu doanh thu, bảng lương và chi phí.");
        }
    }

    public static class MonthlyCloseNotFoundException extends RuntimeException {
        public MonthlyCloseNotFoundException(UUID branchId, LocalDate monthStart) {
            super("Không tìm thấy thông tin chốt sổ cho chi nhánh " + branchId + " kỳ " + monthStart);
        }
    }

    public static class InvalidMonthStartException extends RuntimeException {
        public InvalidMonthStartException(String message) {
            super(message);
        }
    }
}
