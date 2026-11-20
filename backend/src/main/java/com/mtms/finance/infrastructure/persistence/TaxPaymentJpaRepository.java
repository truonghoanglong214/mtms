package com.mtms.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * Repository for tax payments.
 */
@Repository
public interface TaxPaymentJpaRepository extends JpaRepository<TaxPaymentJpaEntity, UUID> {

    List<TaxPaymentJpaEntity> findByObligationIdOrderByPaymentDateDesc(UUID obligationId);

    List<TaxPaymentJpaEntity> findByObligationIdOrderByPaymentDateAscCreatedAtAsc(UUID obligationId);
}
