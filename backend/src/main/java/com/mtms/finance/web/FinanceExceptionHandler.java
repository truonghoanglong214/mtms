package com.mtms.finance.web;

import com.mtms.finance.domain.CannotCancelPaidTaxException;
import com.mtms.finance.domain.DuplicateLedgerPostingException;
import com.mtms.finance.domain.ExpenseAlreadyConfirmedException;
import com.mtms.finance.domain.ExpenseAlreadyVoidedException;
import com.mtms.finance.domain.ExpenseNotFoundException;
import com.mtms.finance.domain.FinancialAccountNotFoundException;
import com.mtms.finance.domain.FinancialCategoryNotFoundException;
import com.mtms.finance.domain.InsufficientAccountBalanceException;
import com.mtms.finance.domain.InvalidCategoryGroupException;
import com.mtms.finance.domain.InvalidCutoverDateException;
import com.mtms.finance.domain.InvalidExpenseCategoryException;
import com.mtms.finance.domain.InvalidReconciliationStatusException;
import com.mtms.finance.domain.InvalidTransferAccountsException;
import com.mtms.finance.domain.NoVarianceToAdjustException;
import com.mtms.finance.domain.OpeningBatchAlreadyExistsException;
import com.mtms.finance.domain.OpeningBatchAlreadyPostedException;
import com.mtms.finance.domain.OpeningBatchNotFoundException;
import com.mtms.finance.domain.PayableAlreadySettledException;
import com.mtms.finance.domain.PayableNotFoundException;
import com.mtms.finance.domain.PaymentExceedsRemainingDebtException;
import com.mtms.finance.domain.ReceivableNotFoundException;
import com.mtms.finance.domain.ReceiptExceedsRemainingReceivableException;
import com.mtms.finance.domain.ReconciliationAlreadyAdjustedException;
import com.mtms.finance.domain.ReconciliationAlreadyExistsException;
import com.mtms.finance.domain.ReconciliationNotFoundException;
import com.mtms.finance.domain.TaxObligationAlreadyExistsException;
import com.mtms.finance.domain.TaxObligationAlreadyPaidException;
import com.mtms.finance.domain.TaxObligationCancelledException;
import com.mtms.finance.domain.TaxObligationNotFoundException;
import com.mtms.finance.domain.TaxPaymentExceedsRemainingLiabilityException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * P0-FIN-01 / P0-FIN-02
 * RFC 7807 Problem Details translation for Finance bounded context exceptions.
 */
@RestControllerAdvice(basePackages = "com.mtms.finance.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FinanceExceptionHandler {

    @ExceptionHandler(InsufficientAccountBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientAccountBalanceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:insufficient-account-balance"));
        problem.setTitle("Insufficient Account Balance");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidTransferAccountsException.class)
    public ProblemDetail handleInvalidTransferAccounts(InvalidTransferAccountsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-transfer-accounts"));
        problem.setTitle("Invalid Transfer Accounts");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidCategoryGroupException.class)
    public ProblemDetail handleInvalidCategoryGroup(InvalidCategoryGroupException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-category-group"));
        problem.setTitle("Invalid Category Group");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FinancialAccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(FinancialAccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:financial-account-not-found"));
        problem.setTitle("Financial Account Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(FinancialCategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(FinancialCategoryNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:financial-category-not-found"));
        problem.setTitle("Financial Category Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateLedgerPostingException.class)
    public ProblemDetail handleDuplicatePosting(DuplicateLedgerPostingException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:duplicate-ledger-posting"));
        problem.setTitle("Duplicate Ledger Posting");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PaymentExceedsRemainingDebtException.class)
    public ProblemDetail handlePaymentExceedsRemainingDebt(PaymentExceedsRemainingDebtException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:payment-exceeds-remaining-debt"));
        problem.setTitle("Payment Exceeds Remaining Debt");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PayableAlreadySettledException.class)
    public ProblemDetail handlePayableAlreadySettled(PayableAlreadySettledException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:payable-already-settled"));
        problem.setTitle("Payable Already Settled");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ReceiptExceedsRemainingReceivableException.class)
    public ProblemDetail handleReceiptExceedsRemainingReceivable(ReceiptExceedsRemainingReceivableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:receipt-exceeds-remaining-receivable"));
        problem.setTitle("Receipt Exceeds Remaining Receivable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PayableNotFoundException.class)
    public ProblemDetail handlePayableNotFound(PayableNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:payable-not-found"));
        problem.setTitle("Payable Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ReceivableNotFoundException.class)
    public ProblemDetail handleReceivableNotFound(ReceivableNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:receivable-not-found"));
        problem.setTitle("Receivable Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidExpenseCategoryException.class)
    public ProblemDetail handleInvalidExpenseCategory(InvalidExpenseCategoryException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-expense-category"));
        problem.setTitle("Invalid Expense Category");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ExpenseAlreadyConfirmedException.class)
    public ProblemDetail handleExpenseAlreadyConfirmed(ExpenseAlreadyConfirmedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:expense-already-confirmed"));
        problem.setTitle("Expense Already Confirmed");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ExpenseAlreadyVoidedException.class)
    public ProblemDetail handleExpenseAlreadyVoided(ExpenseAlreadyVoidedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:expense-already-voided"));
        problem.setTitle("Expense Already Voided");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    public ProblemDetail handleExpenseNotFound(ExpenseNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:expense-not-found"));
        problem.setTitle("Expense Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ReconciliationNotFoundException.class)
    public ProblemDetail handleReconciliationNotFound(ReconciliationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:reconciliation-not-found"));
        problem.setTitle("Reconciliation Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ReconciliationAlreadyExistsException.class)
    public ProblemDetail handleReconciliationAlreadyExists(ReconciliationAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:reconciliation-already-exists"));
        problem.setTitle("Reconciliation Already Exists");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ReconciliationAlreadyAdjustedException.class)
    public ProblemDetail handleReconciliationAlreadyAdjusted(ReconciliationAlreadyAdjustedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:reconciliation-already-adjusted"));
        problem.setTitle("Reconciliation Already Adjusted");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidReconciliationStatusException.class)
    public ProblemDetail handleInvalidReconciliationStatus(InvalidReconciliationStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-reconciliation-status"));
        problem.setTitle("Invalid Reconciliation Status");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NoVarianceToAdjustException.class)
    public ProblemDetail handleNoVarianceToAdjust(NoVarianceToAdjustException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:no-variance-to-adjust"));
        problem.setTitle("No Variance To Adjust");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TaxObligationNotFoundException.class)
    public ProblemDetail handleTaxObligationNotFound(TaxObligationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:tax-obligation-not-found"));
        problem.setTitle("Tax Obligation Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TaxObligationAlreadyExistsException.class)
    public ProblemDetail handleTaxObligationAlreadyExists(TaxObligationAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:tax-obligation-already-exists"));
        problem.setTitle("Tax Obligation Already Exists");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TaxPaymentExceedsRemainingLiabilityException.class)
    public ProblemDetail handleTaxPaymentExceedsRemainingLiability(TaxPaymentExceedsRemainingLiabilityException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:tax-payment-exceeds-remaining-liability"));
        problem.setTitle("Tax Payment Exceeds Remaining Liability");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TaxObligationAlreadyPaidException.class)
    public ProblemDetail handleTaxObligationAlreadyPaid(TaxObligationAlreadyPaidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:tax-obligation-already-paid"));
        problem.setTitle("Tax Obligation Already Paid");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TaxObligationCancelledException.class)
    public ProblemDetail handleTaxObligationCancelled(TaxObligationCancelledException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:tax-obligation-cancelled"));
        problem.setTitle("Tax Obligation Cancelled");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(CannotCancelPaidTaxException.class)
    public ProblemDetail handleCannotCancelPaidTax(CannotCancelPaidTaxException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:cannot-cancel-partially-paid-tax"));
        problem.setTitle("Cannot Cancel Partially Paid Tax");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(OpeningBatchNotFoundException.class)
    public ProblemDetail handleOpeningBatchNotFound(OpeningBatchNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:opening-balance-batch-not-found"));
        problem.setTitle("Opening Balance Batch Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(OpeningBatchAlreadyExistsException.class)
    public ProblemDetail handleOpeningBatchAlreadyExists(OpeningBatchAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:opening-balance-already-exists"));
        problem.setTitle("Opening Balance Already Exists");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(OpeningBatchAlreadyPostedException.class)
    public ProblemDetail handleOpeningBatchAlreadyPosted(OpeningBatchAlreadyPostedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:opening-balance-already-posted"));
        problem.setTitle("Opening Balance Already Posted");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidCutoverDateException.class)
    public ProblemDetail handleInvalidCutoverDate(InvalidCutoverDateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-cutover-date"));
        problem.setTitle("Invalid Cutover Date");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
