package com.mtms.finance.web;

import com.mtms.finance.application.FinancialAccountService;
import com.mtms.finance.application.dto.CreateFinancialAccountCommand;
import com.mtms.finance.application.dto.FinancialAccountDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/finance/accounts")
public class FinancialAccountController {

    private final FinancialAccountService accountService;

    public FinancialAccountController(FinancialAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<List<FinancialAccountDto>> getAccounts(@PathVariable UUID branchId) {
        List<FinancialAccountDto> accounts = accountService.getAccountsByBranch(branchId);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<FinancialAccountDto> createAccount(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateFinancialAccountCommand command
    ) {
        FinancialAccountDto created = accountService.createAccount(branchId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
