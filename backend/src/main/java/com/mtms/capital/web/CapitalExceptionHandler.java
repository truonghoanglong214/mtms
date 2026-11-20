package com.mtms.capital.web;

import com.mtms.capital.domain.CapitalExceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.mtms.capital.web")
public class CapitalExceptionHandler {

    @ExceptionHandler(InvestorNotFoundException.class)
    public ProblemDetail handleInvestorNotFound(InvestorNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(ex.getTypeUri());
        problem.setTitle("Không tìm thấy nhà đầu tư");
        problem.setProperty("investorId", ex.getInvestorId());
        return problem;
    }

    @ExceptionHandler(AgreementNotFoundException.class)
    public ProblemDetail handleAgreementNotFound(AgreementNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(ex.getTypeUri());
        problem.setTitle("Không tìm thấy thỏa thuận đầu tư");
        problem.setProperty("agreementId", ex.getAgreementId());
        return problem;
    }

    @ExceptionHandler(CommitmentNotFoundException.class)
    public ProblemDetail handleCommitmentNotFound(CommitmentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(ex.getTypeUri());
        problem.setTitle("Không tìm thấy cam kết vốn");
        problem.setProperty("commitmentId", ex.getCommitmentId());
        return problem;
    }

    @ExceptionHandler(InvestorConflictException.class)
    public ProblemDetail handleInvestorConflict(InvestorConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(ex.getTypeUri());
        problem.setTitle("Trùng tên hiển thị nhà đầu tư");
        problem.setProperty("displayName", ex.getDisplayName());
        return problem;
    }

    @ExceptionHandler(AgreementConflictException.class)
    public ProblemDetail handleAgreementConflict(AgreementConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(ex.getTypeUri());
        problem.setTitle("Trùng số hợp đồng/thỏa thuận chi nhánh");
        problem.setProperty("agreementRef", ex.getAgreementRef());
        return problem;
    }

    @ExceptionHandler(InvalidContributionException.class)
    public ProblemDetail handleInvalidContribution(InvalidContributionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(ex.getTypeUri());
        problem.setTitle("Thông tin đóng góp vốn không hợp lệ");
        return problem;
    }

    @ExceptionHandler(InvalidProfitTermException.class)
    public ProblemDetail handleInvalidProfitTerm(InvalidProfitTermException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(ex.getTypeUri());
        problem.setTitle("Điều khoản phân chia lợi nhuận không hợp lệ");
        return problem;
    }
}
