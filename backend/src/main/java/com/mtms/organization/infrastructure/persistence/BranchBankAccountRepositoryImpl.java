package com.mtms.organization.infrastructure.persistence;

import com.mtms.organization.domain.BankAccountStatus;
import com.mtms.organization.domain.BranchBankAccount;
import com.mtms.organization.domain.BranchBankAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BranchBankAccountRepositoryImpl implements BranchBankAccountRepository {

    private final BranchBankAccountJpaRepository jpaRepository;

    public BranchBankAccountRepositoryImpl(BranchBankAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BranchBankAccount save(BranchBankAccount account) {
        BranchBankAccountJpaEntity entity = BranchBankAccountJpaEntity.fromDomain(account);
        BranchBankAccountJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<BranchBankAccount> findById(UUID id) {
        return jpaRepository.findById(id).map(BranchBankAccountJpaEntity::toDomain);
    }

    @Override
    public Optional<BranchBankAccount> findByBranchIdAndId(UUID branchId, UUID id) {
        return jpaRepository.findByBranchIdAndId(branchId, id).map(BranchBankAccountJpaEntity::toDomain);
    }

    @Override
    public List<BranchBankAccount> findByBranchId(UUID branchId) {
        return jpaRepository.findByBranchIdOrderByCreatedAtDesc(branchId).stream()
                .map(BranchBankAccountJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<BranchBankAccount> findActiveByBranchId(UUID branchId) {
        return jpaRepository.findByBranchIdAndStatus(branchId, BankAccountStatus.ACTIVE).stream()
                .map(BranchBankAccountJpaEntity::toDomain)
                .toList();
    }

    @Override
    public long countActiveByBranchId(UUID branchId) {
        return jpaRepository.countByBranchIdAndStatus(branchId, BankAccountStatus.ACTIVE);
    }

    @Override
    public void clearPrimaryForBranch(UUID branchId) {
        jpaRepository.clearPrimaryForBranch(branchId);
    }
}
