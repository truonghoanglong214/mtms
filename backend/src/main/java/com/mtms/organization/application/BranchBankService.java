package com.mtms.organization.application;

import com.mtms.organization.application.dto.BranchBankAccountDto;
import com.mtms.organization.application.dto.BranchDto;
import com.mtms.organization.application.dto.BranchReadinessDto;
import com.mtms.organization.application.dto.CreateBankAccountCommand;
import com.mtms.organization.application.exception.BranchNotReadyException;
import com.mtms.organization.application.exception.EntityNotFoundException;
import com.mtms.organization.domain.BankAccountStatus;
import com.mtms.organization.domain.BranchBankAccount;
import com.mtms.organization.domain.BranchBankAccountRepository;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class BranchBankService {

    private final BranchJpaRepository branchRepository;
    private final BranchBankAccountRepository bankAccountRepository;

    public BranchBankService(BranchJpaRepository branchRepository,
                             BranchBankAccountRepository bankAccountRepository) {
        this.branchRepository = branchRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    public BranchBankAccountDto addBankAccount(UUID branchId, CreateBankAccountCommand cmd) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Objects.requireNonNull(cmd, "CreateBankAccountCommand must not be null");

        // 1. Verify branch exists
        if (!branchRepository.existsById(branchId)) {
            throw new EntityNotFoundException("Branch", branchId);
        }

        // 2. Determine primary status
        long activeCount = bankAccountRepository.countActiveByBranchId(branchId);
        boolean shouldBePrimary = cmd.primary() || activeCount == 0;

        if (shouldBePrimary) {
            bankAccountRepository.clearPrimaryForBranch(branchId);
        }

        // 3. Mask and encrypt account number
        String maskedNumber = BankAccountCryptoUtils.mask(cmd.accountNumber());
        String ciphertext = BankAccountCryptoUtils.encrypt(cmd.accountNumber());

        // 4. Create domain entity and save
        BranchBankAccount domain = new BranchBankAccount(
                UUID.randomUUID(),
                branchId,
                cmd.bankName(),
                cmd.accountHolderName(),
                ciphertext,
                maskedNumber,
                shouldBePrimary,
                BankAccountStatus.ACTIVE,
                Instant.now()
        );

        BranchBankAccount saved = bankAccountRepository.save(domain);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<BranchBankAccountDto> listBankAccounts(UUID branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new EntityNotFoundException("Branch", branchId);
        }
        return bankAccountRepository.findByBranchId(branchId).stream()
                .map(this::toDto)
                .toList();
    }

    public BranchBankAccountDto setPrimaryAccount(UUID branchId, UUID accountId) {
        if (!branchRepository.existsById(branchId)) {
            throw new EntityNotFoundException("Branch", branchId);
        }

        BranchBankAccount existing = bankAccountRepository.findByBranchIdAndId(branchId, accountId)
                .orElseThrow(() -> new EntityNotFoundException("BranchBankAccount", accountId));

        if (existing.getStatus() != BankAccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot set inactive bank account as primary");
        }

        // Clear existing primary accounts for this branch
        bankAccountRepository.clearPrimaryForBranch(branchId);

        // Update target account to primary
        BranchBankAccount updated = new BranchBankAccount(
                existing.getId(),
                existing.getBranchId(),
                existing.getBankName(),
                existing.getAccountHolderName(),
                existing.getAccountNumberCiphertext(),
                existing.getMaskedAccountNumber(),
                true,
                existing.getStatus(),
                existing.getCreatedAt()
        );

        BranchBankAccount saved = bankAccountRepository.save(updated);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public BranchReadinessDto checkBranchReadiness(UUID branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new EntityNotFoundException("Branch", branchId);
        }

        long activeAccounts = bankAccountRepository.countActiveByBranchId(branchId);
        List<String> missing = new ArrayList<>();

        if (activeAccounts == 0) {
            missing.add("Branch must have at least one active dedicated bank account before activation");
        }

        boolean ready = missing.isEmpty();
        return new BranchReadinessDto(branchId, ready, missing);
    }

    public BranchDto activateBranch(UUID branchId) {
        BranchReadinessDto readiness = checkBranchReadiness(branchId);
        if (!readiness.readyForActivation()) {
            throw new BranchNotReadyException(branchId, readiness.missingRequirements());
        }

        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Branch", branchId));

        branch.setStatus(BranchStatus.ACTIVE);
        BranchJpaEntity saved = branchRepository.save(branch);

        return new BranchDto(
                saved.getId(),
                saved.getBusinessEntityId(),
                saved.getBranchCode(),
                saved.getName(),
                saved.getTimeZone(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    private BranchBankAccountDto toDto(BranchBankAccount account) {
        return new BranchBankAccountDto(
                account.getId(),
                account.getBranchId(),
                account.getBankName(),
                account.getAccountHolderName(),
                account.getMaskedAccountNumber(),
                account.isPrimary(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}
