package com.mtms.closing.web;

import com.mtms.closing.domain.ClosingExceptions.ClosedPeriodLockedException;
import com.mtms.closing.domain.ClosingExceptions.ClosingPrerequisiteFailedException;
import com.mtms.closing.domain.ClosingExceptions.InvalidMonthStartException;
import com.mtms.closing.domain.ClosingExceptions.MonthAlreadyClosedException;
import com.mtms.closing.domain.ClosingExceptions.MonthlyCloseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.mtms.closing")
public class ClosingExceptionHandler {

    @ExceptionHandler(ClosingPrerequisiteFailedException.class)
    public ProblemDetail handleClosingPrerequisiteFailed(ClosingPrerequisiteFailedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Closing Prerequisite Failed");
        problem.setType(URI.create("urn:mtms:error:closing-prerequisite-failed"));
        return problem;
    }

    @ExceptionHandler(MonthAlreadyClosedException.class)
    public ProblemDetail handleMonthAlreadyClosed(MonthAlreadyClosedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Month Already Closed");
        problem.setType(URI.create("urn:mtms:error:month-already-closed"));
        return problem;
    }

    @ExceptionHandler(ClosedPeriodLockedException.class)
    public ProblemDetail handleClosedPeriodLocked(ClosedPeriodLockedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Closed Period Locked");
        problem.setType(URI.create("urn:mtms:error:closed-period-locked"));
        return problem;
    }

    @ExceptionHandler(MonthlyCloseNotFoundException.class)
    public ProblemDetail handleMonthlyCloseNotFound(MonthlyCloseNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Monthly Close Not Found");
        problem.setType(URI.create("urn:mtms:error:not-found"));
        return problem;
    }

    @ExceptionHandler(InvalidMonthStartException.class)
    public ProblemDetail handleInvalidMonthStart(InvalidMonthStartException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Month Start");
        problem.setType(URI.create("urn:mtms:error:invalid-argument"));
        return problem;
    }
}
