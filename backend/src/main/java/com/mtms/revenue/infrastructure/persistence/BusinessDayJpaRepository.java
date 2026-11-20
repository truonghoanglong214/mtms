package com.mtms.revenue.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessDayJpaRepository extends JpaRepository<BusinessDayJpaEntity, UUID> {

    Optional<BusinessDayJpaEntity> findByBranchIdAndBusinessDate(UUID branchId, LocalDate businessDate);

    List<BusinessDayJpaEntity> findByBranchIdAndBusinessDateBetweenOrderByBusinessDateAsc(
            UUID branchId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<BusinessDayJpaEntity> findByBranchIdOrderByBusinessDateDesc(UUID branchId);
}
