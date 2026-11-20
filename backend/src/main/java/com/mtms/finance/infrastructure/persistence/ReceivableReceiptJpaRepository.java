package com.mtms.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReceivableReceiptJpaRepository extends JpaRepository<ReceivableReceiptJpaEntity, UUID> {

    List<ReceivableReceiptJpaEntity> findByReceivableIdOrderByReceivedOnAscCreatedAtAsc(UUID receivableId);

    List<ReceivableReceiptJpaEntity> findByReceivableId(UUID receivableId);
}
