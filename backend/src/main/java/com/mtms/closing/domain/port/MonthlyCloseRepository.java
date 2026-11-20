package com.mtms.closing.domain.port;

import com.mtms.closing.domain.model.MonthlyClose;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-CLS-01..03 Domain Repository Port
 */
public interface MonthlyCloseRepository {
    MonthlyClose save(MonthlyClose monthlyClose);
    Optional<MonthlyClose> findCurrentByBranchIdAndMonthStart(UUID branchId, LocalDate monthStart);
    List<MonthlyClose> findByBranchIdOrderByMonthStartDesc(UUID branchId);
    boolean existsClosedByBranchIdAndMonthStart(UUID branchId, LocalDate monthStart);
}
