package com.mtms.movement.domain.port;

import com.mtms.movement.domain.model.StockTransfer;
import com.mtms.movement.domain.model.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * P1-MOV-01 / P1-MOV-02
 * Domain repository port for Stock Transfers.
 */
public interface StockTransferRepository {

    StockTransfer save(StockTransfer transfer);

    Optional<StockTransfer> findById(UUID id);

    Optional<StockTransfer> findByIdAndBranch(UUID id, UUID branchId);

    Page<StockTransfer> findOutboundTransfers(
            UUID fromBranchId,
            TransferStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    );

    Page<StockTransfer> findInboundTransfers(
            UUID toBranchId,
            TransferStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    );

    long countByEntityAndMonth(UUID businessEntityId, int year, int month);

    boolean existsById(UUID id);
}
