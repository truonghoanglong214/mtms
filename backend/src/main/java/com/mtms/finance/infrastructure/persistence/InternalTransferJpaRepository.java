package com.mtms.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InternalTransferJpaRepository extends JpaRepository<InternalTransferJpaEntity, UUID> {

    List<InternalTransferJpaEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);
}
