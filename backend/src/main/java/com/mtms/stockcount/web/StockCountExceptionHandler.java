package com.mtms.stockcount.web;

import com.mtms.stockcount.domain.StockCountExceptions.InvalidStockCountStatusException;
import com.mtms.stockcount.domain.StockCountExceptions.MissingDiscrepancyReasonException;
import com.mtms.stockcount.domain.StockCountExceptions.StockCountImmutableException;
import com.mtms.stockcount.domain.StockCountExceptions.StockCountInsufficientStockException;
import com.mtms.stockcount.domain.StockCountExceptions.StockCountNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice(basePackages = "com.mtms.stockcount.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StockCountExceptionHandler {

    @ExceptionHandler(InvalidStockCountStatusException.class)
    public ProblemDetail handleInvalidStockCountStatus(InvalidStockCountStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Stock Count Status");
        problem.setType(URI.create("urn:mtms:error:stock-count-invalid-status"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MissingDiscrepancyReasonException.class)
    public ProblemDetail handleMissingDiscrepancyReason(MissingDiscrepancyReasonException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Missing Discrepancy Reason");
        problem.setType(URI.create("urn:mtms:error:missing-discrepancy-reason"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StockCountInsufficientStockException.class)
    public ProblemDetail handleStockCountInsufficientStock(StockCountInsufficientStockException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Insufficient Stock for Discrepancy (TC-021)");
        problem.setType(URI.create("urn:mtms:error:insufficient-stock"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StockCountImmutableException.class)
    public ProblemDetail handleStockCountImmutable(StockCountImmutableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Stock Count Immutable");
        problem.setType(URI.create("urn:mtms:error:stock-count-immutable"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StockCountNotFoundException.class)
    public ProblemDetail handleStockCountNotFound(StockCountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Stock Count Not Found");
        problem.setType(URI.create("urn:mtms:error:stock-count-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
