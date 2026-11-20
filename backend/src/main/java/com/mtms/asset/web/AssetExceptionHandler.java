package com.mtms.asset.web;

import com.mtms.asset.domain.AssetExceptions.AssetCodeConflictException;
import com.mtms.asset.domain.AssetExceptions.AssetNotFoundException;
import com.mtms.asset.domain.AssetExceptions.InvalidAssetCostException;
import com.mtms.asset.domain.AssetExceptions.InvalidAssetDataException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice(basePackages = "com.mtms.asset.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AssetExceptionHandler {

    @ExceptionHandler(AssetCodeConflictException.class)
    public ProblemDetail handleAssetCodeConflict(AssetCodeConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:asset-code-conflict"));
        problem.setTitle("Asset Code Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AssetNotFoundException.class)
    public ProblemDetail handleAssetNotFound(AssetNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:asset-not-found"));
        problem.setTitle("Asset Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidAssetCostException.class)
    public ProblemDetail handleInvalidAssetCost(InvalidAssetCostException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-asset-cost"));
        problem.setTitle("Invalid Asset Cost");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidAssetDataException.class)
    public ProblemDetail handleInvalidAssetData(InvalidAssetDataException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:mtms:error:invalid-asset-data"));
        problem.setTitle("Invalid Asset Data");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
