package com.mtms.revenue.web;

import com.mtms.revenue.domain.DailyReportNotFoundException;
import com.mtms.revenue.domain.DuplicateDailyReportException;
import com.mtms.revenue.domain.InvalidRevenueMathException;
import com.mtms.revenue.domain.MissingCloseReasonException;
import com.mtms.revenue.domain.ReportAlreadyConfirmedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice(basePackages = "com.mtms.revenue.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RevenueExceptionHandler {

    @ExceptionHandler(InvalidRevenueMathException.class)
    public ProblemDetail handleInvalidRevenueMath(InvalidRevenueMathException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-revenue-math"));
        problem.setTitle("Invalid Revenue Math");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MissingCloseReasonException.class)
    public ProblemDetail handleMissingCloseReason(MissingCloseReasonException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:missing-close-reason"));
        problem.setTitle("Missing Close Reason");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateDailyReportException.class)
    public ProblemDetail handleDuplicateDailyReport(DuplicateDailyReportException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:duplicate-daily-report"));
        problem.setTitle("Duplicate Daily Report");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ReportAlreadyConfirmedException.class)
    public ProblemDetail handleReportAlreadyConfirmed(ReportAlreadyConfirmedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:report-already-confirmed"));
        problem.setTitle("Report Already Confirmed");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DailyReportNotFoundException.class)
    public ProblemDetail handleDailyReportNotFound(DailyReportNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:report-not-found"));
        problem.setTitle("Report Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
