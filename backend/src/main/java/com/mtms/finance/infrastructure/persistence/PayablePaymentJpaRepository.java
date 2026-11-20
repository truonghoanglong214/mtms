package com.mtms.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayablePaymentJpaRepository extends JpaRepository<PayablePaymentJpaEntity, UUID> {

    List<PayablePaymentJpaEntity> findByPayableIdOrderByPaidOnAscCreatedAtAsc(UUID payableId);

    List<PayablePaymentJpaEntity> findByPayableId(UUID payableId);
}
