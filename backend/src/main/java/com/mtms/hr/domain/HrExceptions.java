package com.mtms.hr.domain;

/**
 * P0-HR-01 & P0-HR-02 domain exceptions.
 */
public final class HrExceptions {

    private HrExceptions() {}

    public static class InvalidEmployeeDatesException extends RuntimeException {
        public InvalidEmployeeDatesException(String message) {
            super(message);
        }
    }

    public static class InvalidSalaryPolicyException extends RuntimeException {
        public InvalidSalaryPolicyException(String message) {
            super(message);
        }
    }

    public static class AssignmentConflictException extends RuntimeException {
        public AssignmentConflictException(String message) {
            super(message);
        }
    }

    public static class EmployeeNotFoundException extends RuntimeException {
        public EmployeeNotFoundException(String message) {
            super(message);
        }
    }

    public static class StaffCodeConflictException extends RuntimeException {
        public StaffCodeConflictException(String message) {
            super(message);
        }
    }

    public static class SalaryPolicyNotFoundException extends RuntimeException {
        public SalaryPolicyNotFoundException(String message) {
            super(message);
        }
    }

    public static class SelfApprovalDeniedException extends RuntimeException {
        public SelfApprovalDeniedException(String message) {
            super(message);
        }
    }

    public static class TimesheetNotFoundException extends RuntimeException {
        public TimesheetNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidTimesheetDataException extends RuntimeException {
        public InvalidTimesheetDataException(String message) {
            super(message);
        }
    }

    public static class TimesheetAlreadyApprovedException extends RuntimeException {
        public TimesheetAlreadyApprovedException(String message) {
            super(message);
        }
    }

    public static class PayrollPeriodNotFoundException extends com.mtms.hr.domain.PayrollPeriodNotFoundException {
        public PayrollPeriodNotFoundException(String message) {
            super(message);
        }
    }

    public static class PayrollLineNotFoundException extends com.mtms.hr.domain.PayrollLineNotFoundException {
        public PayrollLineNotFoundException(String message) {
            super(message);
        }
    }

    public static class PayrollAdjustmentNotFoundException extends com.mtms.hr.domain.PayrollAdjustmentNotFoundException {
        public PayrollAdjustmentNotFoundException(String message) {
            super(message);
        }
    }

    public static class PayrollAlreadyApprovedException extends com.mtms.hr.domain.PayrollAlreadyApprovedException {
        public PayrollAlreadyApprovedException(String message) {
            super(message);
        }
    }

    public static class InvalidPayrollDataException extends com.mtms.hr.domain.InvalidPayrollDataException {
        public InvalidPayrollDataException(String message) {
            super(message);
        }
    }

    public static class PayrollPaymentExceedsRemainingException extends com.mtms.hr.domain.PayrollPaymentExceedsRemainingException {
        public PayrollPaymentExceedsRemainingException(String message) {
            super(message);
        }
    }

    public static class PayrollNotApprovedException extends com.mtms.hr.domain.PayrollNotApprovedException {
        public PayrollNotApprovedException(String message) {
            super(message);
        }
    }

    public static class PayrollPaymentNotFoundException extends com.mtms.hr.domain.PayrollPaymentNotFoundException {
        public PayrollPaymentNotFoundException(String message) {
            super(message);
        }
    }
}
