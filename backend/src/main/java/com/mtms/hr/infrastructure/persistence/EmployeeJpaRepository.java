package com.mtms.hr.infrastructure.persistence;

import com.mtms.hr.domain.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {

    boolean existsByStaffCode(String staffCode);

    Optional<EmployeeJpaEntity> findByStaffCode(String staffCode);

    Optional<EmployeeJpaEntity> findByUserId(UUID userId);

    @Query("""
        SELECT DISTINCT e FROM EmployeeJpaEntity e
        JOIN EmployeeAssignmentJpaEntity a ON a.employeeId = e.id
        WHERE a.branchId = :branchId
          AND (:status IS NULL OR e.status = :status)
          AND (:search IS NULL OR :search = '' OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.staffCode) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY e.createdAt DESC
    """)
    Page<EmployeeJpaEntity> findByBranch(
            @Param("branchId") UUID branchId,
            @Param("status") EmployeeStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
