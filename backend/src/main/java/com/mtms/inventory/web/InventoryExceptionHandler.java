package com.mtms.inventory.web;

import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.inventory.application.exception.ConversionNotFoundException;
import com.mtms.inventory.application.exception.DuplicateConversionException;
import com.mtms.inventory.application.exception.DuplicateItemCodeException;
import com.mtms.inventory.application.exception.DuplicateSupplierCodeException;
import com.mtms.inventory.application.exception.DuplicateUnitCodeException;
import com.mtms.inventory.application.exception.ImportBatchNotFoundException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.application.exception.SupplierNotFoundException;
import com.mtms.inventory.application.exception.UnitNotFoundException;
import com.mtms.inventory.domain.InsufficientStockException;
import com.mtms.inventory.domain.InvalidConversionRateException;
import com.mtms.inventory.domain.InvalidIssueQuantityException;
import com.mtms.inventory.domain.InvalidReceiptQuantityException;
import com.mtms.inventory.domain.InvalidStockQuantityException;
import com.mtms.inventory.domain.InvalidTrackingInvariantException;
import com.mtms.inventory.domain.MissingLotCodeException;
import com.mtms.inventory.domain.OpeningInventoryAlreadyPostedException;
import com.mtms.inventory.domain.ReceiptAlreadyPostedException;
import com.mtms.inventory.domain.ReceiptNotFoundException;
import com.mtms.inventory.domain.StockDocumentAlreadyPostedException;
import com.mtms.inventory.domain.StockDocumentNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice(basePackages = "com.mtms.inventory.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InventoryExceptionHandler {

    @ExceptionHandler(InvalidTrackingInvariantException.class)
    public ProblemDetail handleInvalidTrackingInvariant(InvalidTrackingInvariantException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-tracking-invariant"));
        problem.setTitle("Invalid Tracking Invariant");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidConversionRateException.class)
    public ProblemDetail handleInvalidConversionRate(InvalidConversionRateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-conversion-rate"));
        problem.setTitle("Invalid Conversion Rate");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ProblemDetail handleItemNotFound(ItemNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:item-not-found"));
        problem.setTitle("Item Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UnitNotFoundException.class)
    public ProblemDetail handleUnitNotFound(UnitNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:unit-not-found"));
        problem.setTitle("Unit Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ProblemDetail handleSupplierNotFound(SupplierNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:supplier-not-found"));
        problem.setTitle("Supplier Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ReceiptNotFoundException.class)
    public ProblemDetail handleReceiptNotFound(ReceiptNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:receipt-not-found"));
        problem.setTitle("Receipt Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ReceiptAlreadyPostedException.class)
    public ProblemDetail handleReceiptAlreadyPosted(ReceiptAlreadyPostedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:receipt-already-posted"));
        problem.setTitle("Receipt Already Posted");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidReceiptQuantityException.class)
    public ProblemDetail handleInvalidReceiptQuantity(InvalidReceiptQuantityException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-receipt-quantity"));
        problem.setTitle("Invalid Receipt Quantity");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ConversionNotFoundException.class)
    public ProblemDetail handleConversionNotFound(ConversionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:conversion-not-found"));
        problem.setTitle("Conversion Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateItemCodeException.class)
    public ProblemDetail handleDuplicateItemCode(DuplicateItemCodeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:duplicate-item-code"));
        problem.setTitle("Duplicate Item Code");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateConversionException.class)
    public ProblemDetail handleDuplicateConversion(DuplicateConversionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:duplicate-conversion"));
        problem.setTitle("Duplicate Pack Conversion");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateSupplierCodeException.class)
    public ProblemDetail handleDuplicateSupplierCode(DuplicateSupplierCodeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:resource-conflict"));
        problem.setTitle("Duplicate Supplier Code");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateUnitCodeException.class)
    public ProblemDetail handleDuplicateUnitCode(DuplicateUnitCodeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:resource-conflict"));
        problem.setTitle("Duplicate Unit Code");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(OpeningInventoryAlreadyPostedException.class)
    public ProblemDetail handleOpeningInventoryAlreadyPosted(OpeningInventoryAlreadyPostedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:opening-inventory-already-posted"));
        problem.setTitle("Opening Already Posted");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidStockQuantityException.class)
    public ProblemDetail handleInvalidStockQuantity(InvalidStockQuantityException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-stock-quantity"));
        problem.setTitle("Invalid Stock Quantity");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MissingLotCodeException.class)
    public ProblemDetail handleMissingLotCode(MissingLotCodeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:missing-lot-code"));
        problem.setTitle("Missing Lot Code");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ImportBatchNotFoundException.class)
    public ProblemDetail handleImportBatchNotFound(ImportBatchNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:batch-not-found"));
        problem.setTitle("Batch Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public ProblemDetail handleBranchNotFound(BranchNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:branch-not-found"));
        problem.setTitle("Branch Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:insufficient-stock"));
        problem.setTitle("Insufficient Stock");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidIssueQuantityException.class)
    public ProblemDetail handleInvalidIssueQuantity(InvalidIssueQuantityException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-issue-quantity"));
        problem.setTitle("Invalid Issue Quantity");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StockDocumentAlreadyPostedException.class)
    public ProblemDetail handleStockDocumentAlreadyPosted(StockDocumentAlreadyPostedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:document-already-posted"));
        problem.setTitle("Document Already Posted");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StockDocumentNotFoundException.class)
    public ProblemDetail handleStockDocumentNotFound(StockDocumentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:document-not-found"));
        problem.setTitle("Document Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(com.mtms.inventory.application.exception.InvalidDateRangeException.class)
    public ProblemDetail handleInvalidDateRange(com.mtms.inventory.application.exception.InvalidDateRangeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-date-range"));
        problem.setTitle("Invalid Date Range");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
