package com.mtms.organization.infrastructure.persistence;

import com.mtms.organization.domain.BankAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchBankAccountJpaRepository extends JpaRepository<BranchBankAccountJpaEntity, UUID> {

    List<BranchBankAccountJpaEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<BranchBankAccountJpaEntity> findByBranchIdAndStatus(UUID branchId, BankAccountStatus status);

    Optional<BranchBankAccountJpaEntity> findByBranchIdAndId(UUID branchId, UUID id);

    long countByBranchIdAndStatus(UUID branchId, BankAccountStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BranchBankAccountJpaEntity b SET b.isPrimary = false WHERE b.branchId = :branchId")
    void clearPrimaryForBranch(@Param("branchId") UUID branchId);
}
