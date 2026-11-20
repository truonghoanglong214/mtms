package com.mtms.hr.web;

import com.mtms.hr.domain.HrExceptions.AssignmentConflictException;
import com.mtms.hr.domain.HrExceptions.EmployeeNotFoundException;
import com.mtms.hr.domain.HrExceptions.InvalidEmployeeDatesException;
import com.mtms.hr.domain.HrExceptions.InvalidSalaryPolicyException;
import com.mtms.hr.domain.HrExceptions.InvalidTimesheetDataException;
import com.mtms.hr.domain.HrExceptions.SalaryPolicyNotFoundException;
import com.mtms.hr.domain.HrExceptions.SelfApprovalDeniedException;
import com.mtms.hr.domain.HrExceptions.StaffCodeConflictException;
import com.mtms.hr.domain.HrExceptions.TimesheetAlreadyApprovedException;
import com.mtms.hr.domain.HrExceptions.TimesheetNotFoundException;
import com.mtms.hr.domain.InvalidPayrollDataException;
import com.mtms.hr.domain.PayrollAdjustmentNotFoundException;
import com.mtms.hr.domain.PayrollAlreadyApprovedException;
import com.mtms.hr.domain.PayrollLineNotFoundException;
import com.mtms.hr.domain.PayrollPeriodNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * P0-HR-01 & P0-HR-02
 * RFC 7807 Problem Details translation for Workforce HR bounded context exceptions.
 */
@RestControllerAdvice(basePackages = "com.mtms.hr.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HrExceptionHandler {

    @ExceptionHandler(InvalidEmployeeDatesException.class)
    public ProblemDetail handleInvalidEmployeeDates(InvalidEmployeeDatesException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-employee-dates"));
        problem.setTitle("Invalid Employee Dates");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidSalaryPolicyException.class)
    public ProblemDetail handleInvalidSalaryPolicy(InvalidSalaryPolicyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-salary-policy"));
        problem.setTitle("Invalid Salary Policy");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AssignmentConflictException.class)
    public ProblemDetail handleAssignmentConflict(AssignmentConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:assignment-conflict"));
        problem.setTitle("Assignment Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ProblemDetail handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:employee-not-found"));
        problem.setTitle("Employee Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SalaryPolicyNotFoundException.class)
    public ProblemDetail handleSalaryPolicyNotFound(SalaryPolicyNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:employee-not-found"));
        problem.setTitle("Salary Policy Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StaffCodeConflictException.class)
    public ProblemDetail handleStaffCodeConflict(StaffCodeConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:staff-code-conflict"));
        problem.setTitle("Staff Code Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SelfApprovalDeniedException.class)
    public ProblemDetail handleSelfApprovalDenied(SelfApprovalDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:self-approval-denied"));
        problem.setTitle("Self Approval Denied");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TimesheetNotFoundException.class)
    public ProblemDetail handleTimesheetNotFound(TimesheetNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:timesheet-not-found"));
        problem.setTitle("Timesheet Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidTimesheetDataException.class)
    public ProblemDetail handleInvalidTimesheetData(InvalidTimesheetDataException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-timesheet-data"));
        problem.setTitle("Invalid Timesheet Data");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TimesheetAlreadyApprovedException.class)
    public ProblemDetail handleTimesheetAlreadyApproved(TimesheetAlreadyApprovedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:timesheet-already-approved"));
        problem.setTitle("Timesheet Already Approved");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({PayrollPeriodNotFoundException.class, PayrollLineNotFoundException.class, PayrollAdjustmentNotFoundException.class, com.mtms.hr.domain.PayrollPaymentNotFoundException.class})
    public ProblemDetail handlePayrollNotFound(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:payroll-not-found"));
        problem.setTitle("Payroll Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(com.mtms.hr.domain.PayrollPaymentExceedsRemainingException.class)
    public ProblemDetail handlePayrollPaymentExceedsRemaining(com.mtms.hr.domain.PayrollPaymentExceedsRemainingException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:payroll-payment-exceeds-remaining"));
        problem.setTitle("Payroll Payment Exceeds Remaining");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(com.mtms.hr.domain.PayrollNotApprovedException.class)
    public ProblemDetail handlePayrollNotApproved(com.mtms.hr.domain.PayrollNotApprovedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:payroll-not-approved"));
        problem.setTitle("Payroll Not Approved");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(com.mtms.finance.domain.InsufficientAccountBalanceException.class)
    public ProblemDetail handleInsufficientAccountBalance(com.mtms.finance.domain.InsufficientAccountBalanceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:insufficient-funds"));
        problem.setTitle("Insufficient Account Balance");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(com.mtms.finance.domain.FinancialAccountNotFoundException.class)
    public ProblemDetail handleFinancialAccountNotFound(com.mtms.finance.domain.FinancialAccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:account-not-found"));
        problem.setTitle("Financial Account Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(com.mtms.common.error.AccessForbiddenException.class)
    public ProblemDetail handleAccessForbidden(com.mtms.common.error.AccessForbiddenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:forbidden"));
        problem.setTitle("Access Forbidden");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PayrollAlreadyApprovedException.class)
    public ProblemDetail handlePayrollAlreadyApproved(PayrollAlreadyApprovedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:payroll-already-approved"));
        problem.setTitle("Payroll Already Approved");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidPayrollDataException.class)
    public ProblemDetail handleInvalidPayrollData(InvalidPayrollDataException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-payroll-data"));
        problem.setTitle("Invalid Payroll Data");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
