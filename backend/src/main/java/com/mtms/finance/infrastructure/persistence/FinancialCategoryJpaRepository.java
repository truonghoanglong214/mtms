package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.FlowDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialCategoryJpaRepository extends JpaRepository<FinancialCategoryJpaEntity, UUID> {

    Optional<FinancialCategoryJpaEntity> findByCategoryCode(String categoryCode);

    boolean existsByCategoryCode(String categoryCode);

    @Query("SELECT c FROM FinancialCategoryJpaEntity c WHERE " +
           "(:categoryGroup IS NULL OR c.categoryGroup = :categoryGroup) " +
           "AND (:flowDirection IS NULL OR c.flowDirection = :flowDirection OR c.flowDirection = com.mtms.finance.domain.FlowDirection.BOTH) " +
           "AND (:activeOnly = false OR c.isActive = true) " +
           "ORDER BY c.isSystem DESC, c.categoryCode ASC")
    List<FinancialCategoryJpaEntity> findCategories(
            @Param("categoryGroup") CategoryGroup categoryGroup,
            @Param("flowDirection") FlowDirection flowDirection,
            @Param("activeOnly") boolean activeOnly
    );
}
