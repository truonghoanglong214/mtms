package com.mtms.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScopedRoleGrantJpaRepository extends JpaRepository<ScopedRoleGrantJpaEntity, UUID> {

    List<ScopedRoleGrantJpaEntity> findByUserId(UUID userId);

    List<ScopedRoleGrantJpaEntity> findByBranchId(UUID branchId);

    List<ScopedRoleGrantJpaEntity> findByBusinessEntityId(UUID businessEntityId);

    @Query("SELECT g FROM ScopedRoleGrantJpaEntity g WHERE g.userId = :userId " +
           "AND g.validFrom <= :date AND (g.validTo IS NULL OR g.validTo >= :date)")
    List<ScopedRoleGrantJpaEntity> findActiveGrantsByUserId(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date
    );

    @Query("SELECT g FROM ScopedRoleGrantJpaEntity g WHERE g.userId = :userId " +
           "AND (g.branchId = :branchId OR g.roleKey = 'SYSTEM_ADMIN') " +
           "AND g.validFrom <= :date AND (g.validTo IS NULL OR g.validTo >= :date)")
    List<ScopedRoleGrantJpaEntity> findActiveGrantsByUserIdAndBranchId(
            @Param("userId") UUID userId,
            @Param("branchId") UUID branchId,
            @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(g) > 0 FROM ScopedRoleGrantJpaEntity g WHERE g.userId = :userId " +
           "AND (g.branchId = :branchId OR g.roleKey = 'SYSTEM_ADMIN') " +
           "AND g.validFrom <= :date AND (g.validTo IS NULL OR g.validTo >= :date)")
    boolean hasActiveBranchAccess(
            @Param("userId") UUID userId,
            @Param("branchId") UUID branchId,
            @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(g) > 0 FROM ScopedRoleGrantJpaEntity g WHERE g.userId = :userId " +
           "AND g.roleKey = 'SYSTEM_ADMIN' " +
           "AND g.validFrom <= :date AND (g.validTo IS NULL OR g.validTo >= :date)")
    boolean isSystemAdmin(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date
    );
}
