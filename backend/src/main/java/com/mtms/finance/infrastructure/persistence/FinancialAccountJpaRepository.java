package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.AccountKind;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialAccountJpaRepository extends JpaRepository<FinancialAccountJpaEntity, UUID> {

    List<FinancialAccountJpaEntity> findByBranchIdOrderByCreatedAtAsc(UUID branchId);

    Optional<FinancialAccountJpaEntity> findByBranchIdAndAccountKind(UUID branchId, AccountKind accountKind);

    Optional<FinancialAccountJpaEntity> findByBranchIdAndAccountName(UUID branchId, String accountName);

    Optional<FinancialAccountJpaEntity> findByIdAndBranchId(UUID id, UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM FinancialAccountJpaEntity a WHERE a.id = :id")
    Optional<FinancialAccountJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
