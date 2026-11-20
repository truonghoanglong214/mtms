package com.mtms.movement.infrastructure.persistence;

import com.mtms.movement.domain.model.StockTransfer;
import com.mtms.movement.domain.model.StockTransferLine;
import com.mtms.movement.domain.model.TransferStatus;
import com.mtms.movement.domain.port.StockTransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StockTransferRepositoryAdapter implements StockTransferRepository {

    private final SpringDataStockTransferRepository jpaRepository;

    public StockTransferRepositoryAdapter(SpringDataStockTransferRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockTransfer save(StockTransfer transfer) {
        StockTransferJpaEntity entity = toEntity(transfer);
        StockTransferJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<StockTransfer> findById(UUID id) {
        return jpaRepository.findByIdWithLines(id).map(this::toDomain);
    }

    @Override
    public Optional<StockTransfer> findByIdAndBranch(UUID id, UUID branchId) {
        return jpaRepository.findByIdAndBranchWithLines(id, branchId).map(this::toDomain);
    }

    @Override
    public Page<StockTransfer> findOutboundTransfers(
            UUID fromBranchId,
            TransferStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        return jpaRepository.findOutboundWithFilters(fromBranchId, status, startDate, endDate, search, pageable)
                .map(this::toDomain);
    }

    @Override
    public Page<StockTransfer> findInboundTransfers(
            UUID toBranchId,
            TransferStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        return jpaRepository.findInboundWithFilters(toBranchId, status, startDate, endDate, search, pageable)
                .map(this::toDomain);
    }

    @Override
    public long countByEntityAndMonth(UUID businessEntityId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return jpaRepository.countByBusinessEntityIdAndRequestedOnBetween(
                businessEntityId, ym.atDay(1), ym.atEndOfMonth()
        );
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    private StockTransfer toDomain(StockTransferJpaEntity entity) {
        List<StockTransferLine> domainLines = new ArrayList<>();
        if (entity.getLines() != null) {
            for (StockTransferLineJpaEntity le : entity.getLines()) {
                domainLines.add(new StockTransferLine(
                        le.getId(),
                        entity.getId(),
                        le.getLineNumber(),
                        le.getItemId(),
                        le.getLotId(),
                        le.getDispatchedBaseQty(),
                        le.getReceivedBaseQty(),
                        le.getTransitVarianceQty(),
                        le.getSenderUnitCost(),
                        le.getDispatchedLineCost(),
                        le.getReceivedLineCost(),
                        le.getReceiverNote(),
                        le.getNotes(),
                        le.getCreatedAt()
                ));
            }
        }

        return new StockTransfer(
                entity.getId(),
                entity.getTransferNumber(),
                entity.getBusinessEntityId(),
                entity.getFromBranchId(),
                entity.getToBranchId(),
                entity.getRequestedOn(),
                entity.getDispatchedAt(),
                entity.getCompletedAt(),
                entity.getStatus(),
                entity.getRequestedBy(),
                entity.getDispatchedBy(),
                entity.getReceivedBy(),
                entity.getTotalLines(),
                entity.getTotalDispatchedQuantity(),
                entity.getTotalReceivedQuantity(),
                entity.getTotalTransitVarianceQuantity(),
                entity.getTotalDispatchedCost(),
                entity.getTotalReceivedCost(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                domainLines
        );
    }

    private StockTransferJpaEntity toEntity(StockTransfer domain) {
        StockTransferJpaEntity entity = new StockTransferJpaEntity(
                domain.getId(),
                domain.getTransferNumber(),
                domain.getBusinessEntityId(),
                domain.getFromBranchId(),
                domain.getToBranchId(),
                domain.getRequestedOn(),
                domain.getDispatchedAt(),
                domain.getCompletedAt(),
                domain.getStatus(),
                domain.getRequestedBy(),
                domain.getDispatchedBy(),
                domain.getReceivedBy(),
                domain.getTotalLines(),
                domain.getTotalDispatchedQuantity(),
                domain.getTotalReceivedQuantity(),
                domain.getTotalTransitVarianceQuantity(),
                domain.getTotalDispatchedCost(),
                domain.getTotalReceivedCost(),
                domain.getNotes(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );

        List<StockTransferLineJpaEntity> lineEntities = new ArrayList<>();
        if (domain.getLines() != null) {
            for (StockTransferLine dl : domain.getLines()) {
                lineEntities.add(new StockTransferLineJpaEntity(
                        dl.getId(),
                        entity,
                        dl.getLineNumber(),
                        dl.getItemId(),
                        dl.getLotId(),
                        dl.getDispatchedBaseQty(),
                        dl.getReceivedBaseQty(),
                        dl.getTransitVarianceQty(),
                        dl.getSenderUnitCost(),
                        dl.getDispatchedLineCost(),
                        dl.getReceivedLineCost(),
                        dl.getReceiverNote(),
                        dl.getNotes(),
                        dl.getCreatedAt()
                ));
            }
        }
        entity.setLines(lineEntities);
        return entity;
    }
}
