package com.mtms.finance.application;

import com.mtms.common.decimal.MoneyUtils;
import com.mtms.finance.application.dto.CreateFinancialAccountCommand;
import com.mtms.finance.application.dto.FinancialAccountDto;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.FinancialAccount;
import com.mtms.finance.domain.FinancialAccountNotFoundException;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.organization.domain.BranchBankAccount;
import com.mtms.organization.domain.BranchBankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinancialAccountService {

    private final FinancialAccountJpaRepository accountRepository;
    private final BranchBankAccountRepository branchBankAccountRepository;

    public FinancialAccountService(
            FinancialAccountJpaRepository accountRepository,
            @Autowired(required = false) BranchBankAccountRepository branchBankAccountRepository
    ) {
        this.accountRepository = accountRepository;
        this.branchBankAccountRepository = branchBankAccountRepository;
    }

    @Transactional
    public List<FinancialAccountDto> getAccountsByBranch(UUID branchId) {
        ensureDefaultCashAccount(branchId);
        List<FinancialAccountJpaEntity> entities = accountRepository.findByBranchIdOrderByCreatedAtAsc(branchId);
        return entities.stream().map(this::toDto).toList();
    }

    @Transactional
    public FinancialAccountJpaEntity ensureDefaultCashAccount(UUID branchId) {
        Optional<FinancialAccountJpaEntity> existing = accountRepository.findByBranchIdAndAccountKind(branchId, AccountKind.CASH);
        if (existing.isPresent()) {
            return existing.get();
        }

        FinancialAccount cashAccount = FinancialAccount.createDefaultCashAccount(UUID.randomUUID(), branchId);
        return accountRepository.save(FinancialAccountJpaEntity.fromDomain(cashAccount));
    }

    @Transactional
    public FinancialAccountJpaEntity ensureDefaultBankAccount(UUID branchId) {
        Optional<FinancialAccountJpaEntity> existing = accountRepository.findByBranchIdAndAccountKind(branchId, AccountKind.BANK);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Try to link with primary or first dedicated bank account of the branch if available
        UUID bankAccountId = null;
        String accountName = "Tài khoản ngân hàng chi nhánh";
        if (branchBankAccountRepository != null) {
            try {
                List<BranchBankAccount> activeAccounts = branchBankAccountRepository.findActiveByBranchId(branchId);
                if (!activeAccounts.isEmpty()) {
                    BranchBankAccount selected = activeAccounts.stream()
                            .filter(BranchBankAccount::isPrimary)
                            .findFirst()
                            .orElse(activeAccounts.get(0));
                    bankAccountId = selected.getId();
                    accountName = selected.getBankName() + " (" + selected.getMaskedAccountNumber() + ")";
                }
            } catch (Exception ignored) {
            }
        }

        FinancialAccount bankAccount = new FinancialAccount(
                UUID.randomUUID(),
                branchId,
                bankAccountId,
                accountName,
                AccountKind.BANK,
                "VND",
                MoneyUtils.ZERO_MONEY,
                AccountStatus.ACTIVE,
                Instant.now()
        );
        return accountRepository.save(FinancialAccountJpaEntity.fromDomain(bankAccount));
    }

    @Transactional
    public FinancialAccountDto createAccount(UUID branchId, CreateFinancialAccountCommand command) {
        if (command.accountKind() == AccountKind.CASH) {
            Optional<FinancialAccountJpaEntity> existing = accountRepository.findByBranchIdAndAccountKind(branchId, AccountKind.CASH);
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Chi nhánh đã có tài khoản quỹ tiền mặt");
            }
        }

        if (command.accountKind() == AccountKind.BANK && command.bankAccountId() == null) {
            throw new IllegalArgumentException("Tài khoản ngân hàng phải liên kết với một tài khoản ngân hàng chi nhánh (bankAccountId)");
        }

        FinancialAccount account;
        if (command.accountKind() == AccountKind.CASH) {
            account = FinancialAccount.createDefaultCashAccount(UUID.randomUUID(), branchId);
        } else {
            account = FinancialAccount.createBankAccount(
                    UUID.randomUUID(),
                    branchId,
                    command.bankAccountId(),
                    command.accountName()
            );
        }

        FinancialAccountJpaEntity saved = accountRepository.save(FinancialAccountJpaEntity.fromDomain(account));
        return toDto(saved);
    }

    public FinancialAccountJpaEntity getAccountEntity(UUID accountId, UUID branchId) {
        return accountRepository.findByIdAndBranchId(accountId, branchId)
                .orElseThrow(() -> new FinancialAccountNotFoundException(accountId));
    }

    public FinancialAccountDto toDto(FinancialAccountJpaEntity entity) {
        return new FinancialAccountDto(
                entity.getId(),
                entity.getBranchId(),
                entity.getBankAccountId(),
                entity.getAccountName(),
                entity.getAccountKind(),
                entity.getCurrency(),
                entity.getCurrentBalance(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
