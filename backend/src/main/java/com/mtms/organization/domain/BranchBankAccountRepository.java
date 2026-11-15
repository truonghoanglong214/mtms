package com.mtms.organization.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-ORG-07 / BR-ORG-03
 * Domain repository contract for branch bank accounts.
 */
public interface BranchBankAccountRepository {

    BranchBankAccount save(BranchBankAccount account);

    Optional<BranchBankAccount> findById(UUID id);

    Optional<BranchBankAccount> findByBranchIdAndId(UUID branchId, UUID id);

    List<BranchBankAccount> findByBranchId(UUID branchId);

    List<BranchBankAccount> findActiveByBranchId(UUID branchId);

    long countActiveByBranchId(UUID branchId);

    void clearPrimaryForBranch(UUID branchId);
}
