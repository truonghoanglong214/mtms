package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.SupplierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, UUID> {

    Optional<SupplierJpaEntity> findByBusinessEntityIdAndId(UUID businessEntityId, UUID id);

    Optional<SupplierJpaEntity> findByBusinessEntityIdAndSupplierCode(UUID businessEntityId, String supplierCode);

    List<SupplierJpaEntity> findByBusinessEntityIdAndStatusOrderBySupplierCodeAsc(UUID businessEntityId, SupplierStatus status);

    List<SupplierJpaEntity> findByBusinessEntityIdOrderBySupplierCodeAsc(UUID businessEntityId);

    boolean existsByBusinessEntityIdAndSupplierCode(UUID businessEntityId, String supplierCode);
}
