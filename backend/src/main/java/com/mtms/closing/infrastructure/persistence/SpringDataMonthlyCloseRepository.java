package com.mtms.closing.infrastructure.persistence;

import com.mtms.closing.domain.model.CloseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMonthlyCloseRepository extends JpaRepository<MonthlyCloseJpaEntity, UUID> {

    Optional<MonthlyCloseJpaEntity> findByBranchIdAndMonthStartAndIsCurrentTrue(UUID branchId, LocalDate monthStart);

    List<MonthlyCloseJpaEntity> findByBranchIdOrderByMonthStartDescRevisionDesc(UUID branchId);

    boolean existsByBranchIdAndMonthStartAndStatus(UUID branchId, LocalDate monthStart, CloseStatus status);
}
