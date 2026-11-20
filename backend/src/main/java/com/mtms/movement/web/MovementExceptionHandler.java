package com.mtms.movement.web;

import com.mtms.movement.domain.MovementExceptions.InterEntityTransferForbiddenException;
import com.mtms.movement.domain.MovementExceptions.InvalidTransferStatusException;
import com.mtms.movement.domain.MovementExceptions.SameBranchTransferForbiddenException;
import com.mtms.movement.domain.MovementExceptions.TransferInsufficientStockException;
import com.mtms.movement.domain.MovementExceptions.TransferNotFoundException;
import com.mtms.movement.domain.MovementExceptions.TransferOverReceiptException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice(basePackages = "com.mtms.movement.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MovementExceptionHandler {

    @ExceptionHandler(InterEntityTransferForbiddenException.class)
    public ProblemDetail handleInterEntityTransferForbidden(InterEntityTransferForbiddenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Inter-Entity Transfer Forbidden (TC-028)");
        problem.setType(URI.create("urn:mtms:error:inter-entity-transfer-forbidden"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SameBranchTransferForbiddenException.class)
    public ProblemDetail handleSameBranchTransferForbidden(SameBranchTransferForbiddenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Same Branch Transfer Forbidden");
        problem.setType(URI.create("urn:mtms:error:same-branch-transfer-forbidden"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TransferInsufficientStockException.class)
    public ProblemDetail handleTransferInsufficientStock(TransferInsufficientStockException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Insufficient Stock for Transfer (TC-021)");
        problem.setType(URI.create("urn:mtms:error:insufficient-stock"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TransferOverReceiptException.class)
    public ProblemDetail handleTransferOverReceipt(TransferOverReceiptException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Transfer Over Receipt Denied");
        problem.setType(URI.create("urn:mtms:error:over-receipt-denied"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidTransferStatusException.class)
    public ProblemDetail handleInvalidTransferStatus(InvalidTransferStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Transfer Status");
        problem.setType(URI.create("urn:mtms:error:invalid-transfer-status"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TransferNotFoundException.class)
    public ProblemDetail handleTransferNotFound(TransferNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Transfer Not Found");
        problem.setType(URI.create("urn:mtms:error:transfer-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
