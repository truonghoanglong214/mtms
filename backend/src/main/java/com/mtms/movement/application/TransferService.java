package com.mtms.movement.application;

import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.domain.StockBalance;
import com.mtms.inventory.domain.StockBalanceRepository;
import com.mtms.inventory.domain.StockLot;
import com.mtms.inventory.domain.StockLotRepository;
import com.mtms.inventory.domain.StockMovement;
import com.mtms.inventory.domain.StockMovementRepository;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.movement.application.dto.TransferDtos.CreateTransferLineRequest;
import com.mtms.movement.application.dto.TransferDtos.CreateTransferRequest;
import com.mtms.movement.application.dto.TransferDtos.ReceiveTransferLineRequest;
import com.mtms.movement.application.dto.TransferDtos.ReceiveTransferRequest;
import com.mtms.movement.application.dto.TransferDtos.TransferDetailDto;
import com.mtms.movement.application.dto.TransferDtos.TransferLineDto;
import com.mtms.movement.application.dto.TransferDtos.TransferListResponse;
import com.mtms.movement.application.dto.TransferDtos.TransferSummaryDto;
import com.mtms.movement.domain.MovementExceptions.InterEntityTransferForbiddenException;
import com.mtms.movement.domain.MovementExceptions.InvalidTransferStatusException;
import com.mtms.movement.domain.MovementExceptions.SameBranchTransferForbiddenException;
import com.mtms.movement.domain.MovementExceptions.TransferInsufficientStockException;
import com.mtms.movement.domain.MovementExceptions.TransferNotFoundException;
import com.mtms.movement.domain.MovementExceptions.TransferOverReceiptException;
import com.mtms.movement.domain.model.StockTransfer;
import com.mtms.movement.domain.model.StockTransferLine;
import com.mtms.movement.domain.model.TransferStatus;
import com.mtms.movement.domain.port.StockTransferRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final StockTransferRepository stockTransferRepository;
    private final BranchJpaRepository branchRepository;
    private final CatalogItemJpaRepository catalogItemRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLotRepository stockLotRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserJpaRepository userRepository;

    public TransferService(
            StockTransferRepository stockTransferRepository,
            BranchJpaRepository branchRepository,
            CatalogItemJpaRepository catalogItemRepository,
            StockBalanceRepository stockBalanceRepository,
            StockLotRepository stockLotRepository,
            StockMovementRepository stockMovementRepository,
            UserJpaRepository userRepository
    ) {
        this.stockTransferRepository = stockTransferRepository;
        this.branchRepository = branchRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockLotRepository = stockLotRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TransferDetailDto createTransfer(
            UUID fromBranchId,
            CreateTransferRequest command,
            UUID actorUserId
    ) {
        BranchJpaEntity fromBranch = branchRepository.findById(fromBranchId)
                .orElseThrow(() -> new BranchNotFoundException(fromBranchId));

        BranchJpaEntity toBranch = branchRepository.findById(command.toBranchId())
                .orElseThrow(() -> new BranchNotFoundException(command.toBranchId()));

        // Check 1: Same branch transfer forbidden
        if (fromBranchId.equals(command.toBranchId())) {
            throw new SameBranchTransferForbiddenException("Chi nhánh gửi và chi nhánh nhận không được trùng nhau.");
        }

        // Check 2: TC-028 - Inter-entity transfer is strictly forbidden
        if (!fromBranch.getBusinessEntityId().equals(toBranch.getBusinessEntityId())) {
            throw new InterEntityTransferForbiddenException(
                    "Cấm điều chuyển nội bộ giữa hai pháp nhân khác nhau (TC-028). Chi nhánh gửi '" +
                    fromBranch.getName() + "' và chi nhánh nhận '" + toBranch.getName() +
                    "' thuộc các pháp nhân riêng biệt. Vui lòng sử dụng luồng vay mượn đối tác hoặc bán nguyên vật liệu."
            );
        }

        UUID businessEntityId = fromBranch.getBusinessEntityId();
        int year = command.requestedOn().getYear();
        int month = command.requestedOn().getMonthValue();
        long count = stockTransferRepository.countByEntityAndMonth(businessEntityId, year, month);
        String transferNumber = String.format("CK-%04d%02d-%04d", year, month, count + 1);

        UUID transferId = UUID.randomUUID();
        List<StockTransferLine> lines = new ArrayList<>();
        int lineNo = 1;

        for (CreateTransferLineRequest lineCmd : command.lines()) {
            CatalogItemJpaEntity itemEntity = catalogItemRepository.findById(lineCmd.itemId())
                    .orElseThrow(() -> new ItemNotFoundException(lineCmd.itemId()));

            if (!itemEntity.getBusinessEntityId().equals(businessEntityId)) {
                throw new IllegalArgumentException("Mặt hàng " + itemEntity.getName() + " không thuộc cùng pháp nhân.");
            }

            if (lineCmd.lotId() != null) {
                StockLot lot = stockLotRepository.findById(lineCmd.lotId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô hàng: " + lineCmd.lotId()));
                if (!lot.getBranchId().equals(fromBranchId) || !lot.getItemId().equals(lineCmd.itemId())) {
                    throw new IllegalArgumentException("Lô hàng không khớp với chi nhánh gửi hoặc mặt hàng.");
                }
            }

            StockTransferLine line = StockTransferLine.createDraftLine(
                    transferId,
                    lineNo++,
                    lineCmd.itemId(),
                    lineCmd.lotId(),
                    lineCmd.dispatchedBaseQty(),
                    lineCmd.notes()
            );
            lines.add(line);
        }

        StockTransfer transfer = StockTransfer.createDraft(
                transferId,
                transferNumber,
                businessEntityId,
                fromBranchId,
                command.toBranchId(),
                command.requestedOn(),
                command.notes(),
                actorUserId,
                lines
        );

        StockTransfer saved = stockTransferRepository.save(transfer);

        if (Boolean.TRUE.equals(command.autoDispatch())) {
            saved = dispatchTransferInternal(saved, fromBranch, actorUserId);
        }

        return toDetailDto(saved, fromBranch, toBranch);
    }

    @Transactional
    public TransferDetailDto dispatchTransfer(
            UUID fromBranchId,
            UUID transferId,
            UUID actorUserId
    ) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        if (!transfer.getFromBranchId().equals(fromBranchId)) {
            throw new IllegalArgumentException("Phiếu chuyển kho không xuất phát từ chi nhánh này.");
        }

        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new InvalidTransferStatusException(
                    "Chỉ có thể xuất kho phiếu đang ở trạng thái DRAFT. Trạng thái hiện tại: " + transfer.getStatus()
            );
        }

        UUID toBranchId = transfer.getToBranchId();
        BranchJpaEntity fromBranch = branchRepository.findById(fromBranchId)
                .orElseThrow(() -> new BranchNotFoundException(fromBranchId));
        BranchJpaEntity toBranch = branchRepository.findById(toBranchId)
                .orElseThrow(() -> new BranchNotFoundException(toBranchId));

        StockTransfer dispatched = dispatchTransferInternal(transfer, fromBranch, actorUserId);
        return toDetailDto(dispatched, fromBranch, toBranch);
    }

    private StockTransfer dispatchTransferInternal(
            StockTransfer transfer,
            BranchJpaEntity fromBranch,
            UUID actorUserId
    ) {
        for (StockTransferLine line : transfer.getLines()) {
            UUID itemId = line.getItemId();
            BigDecimal dispatchedQty = line.getDispatchedBaseQty();

            // A. Row Lock sender stock balance (TC-021)
            StockBalance balance = stockBalanceRepository.findWithLockByBranchIdAndItemId(fromBranch.getId(), itemId)
                    .orElseThrow(() -> {
                        CatalogItemJpaEntity itemEntity = catalogItemRepository.findById(itemId).orElse(null);
                        String itemName = itemEntity != null ? itemEntity.getName() : itemId.toString();
                        return new TransferInsufficientStockException(
                                "Tồn kho không đủ để xuất điều chuyển mặt hàng: " + itemName +
                                " tại chi nhánh " + fromBranch.getName() + " (TC-021). Tồn khả dụng: 0, Yêu cầu xuất: " + dispatchedQty
                        );
                    });

            if (balance.getQuantity().compareTo(dispatchedQty) < 0) {
                CatalogItemJpaEntity itemEntity = catalogItemRepository.findById(itemId).orElse(null);
                String itemName = itemEntity != null ? itemEntity.getName() : itemId.toString();
                throw new TransferInsufficientStockException(
                        "Tồn kho không đủ để xuất điều chuyển mặt hàng: " + itemName +
                        " tại chi nhánh " + fromBranch.getName() + " (TC-021). Tồn khả dụng: " +
                        balance.getQuantity() + ", Yêu cầu xuất: " + dispatchedQty
                );
            }

            // B. Snapshot sender Moving WAC and deduct stock
            BigDecimal senderWac = balance.getAvgUnitCost();
            BigDecimal bookedCost = balance.issueStock(dispatchedQty);
            stockBalanceRepository.save(balance);

            // Deduct lot if applicable
            if (line.getLotId() != null) {
                StockLot lot = stockLotRepository.findById(line.getLotId()).orElse(null);
                if (lot != null && lot.getQuantity().compareTo(dispatchedQty) >= 0) {
                    lot.setQuantity(lot.getQuantity().subtract(dispatchedQty));
                    stockLotRepository.save(lot);
                }
            }

            line.dispatch(senderWac);

            // C. Create immutable TRANSFER_OUT movement
            StockMovement movement = StockMovement.createTransferOutMovement(
                    fromBranch.getBusinessEntityId(),
                    fromBranch.getId(),
                    itemId,
                    line.getLotId(),
                    transfer.getRequestedOn(),
                    dispatchedQty,
                    bookedCost,
                    senderWac,
                    transfer.getId(),
                    transfer.getTransferNumber(),
                    actorUserId
            );
            stockMovementRepository.save(movement);
        }

        transfer.markDispatched(actorUserId);
        return stockTransferRepository.save(transfer);
    }

    @Transactional
    public TransferDetailDto receiveTransfer(
            UUID toBranchId,
            UUID transferId,
            ReceiveTransferRequest command,
            UUID actorUserId
    ) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        if (!transfer.getToBranchId().equals(toBranchId)) {
            throw new IllegalArgumentException("Phiếu chuyển kho này không chuyển đến chi nhánh hiện tại.");
        }

        if (transfer.getStatus() != TransferStatus.DISPATCHED) {
            throw new InvalidTransferStatusException(
                    "Chỉ có thể tiếp nhận phiếu đang ở trạng thái DISPATCHED. Trạng thái hiện tại: " + transfer.getStatus()
            );
        }

        UUID fromBranchId = transfer.getFromBranchId();
        BranchJpaEntity toBranch = branchRepository.findById(toBranchId)
                .orElseThrow(() -> new BranchNotFoundException(toBranchId));
        BranchJpaEntity fromBranch = branchRepository.findById(fromBranchId)
                .orElseThrow(() -> new BranchNotFoundException(fromBranchId));

        Map<UUID, ReceiveTransferLineRequest> receiptMap = command.lines().stream()
                .collect(Collectors.toMap(ReceiveTransferLineRequest::lineId, r -> r));

        LocalDate receiptDate = LocalDate.now();

        for (StockTransferLine line : transfer.getLines()) {
            ReceiveTransferLineRequest lineReceipt = receiptMap.get(line.getId());
            BigDecimal receivedQty;
            String receiverNote = null;

            if (lineReceipt != null) {
                receivedQty = lineReceipt.receivedBaseQty();
                receiverNote = lineReceipt.receiverNote();
            } else {
                // If line not explicitly received, default to full receipt
                receivedQty = line.getDispatchedBaseQty();
            }

            if (receivedQty.compareTo(line.getDispatchedBaseQty()) > 0) {
                CatalogItemJpaEntity itemEntity = catalogItemRepository.findById(line.getItemId()).orElse(null);
                String itemName = itemEntity != null ? itemEntity.getName() : line.getItemId().toString();
                throw new TransferOverReceiptException(
                        "Số lượng thực nhận (" + receivedQty + ") không được vượt quá số lượng xuất (" +
                        line.getDispatchedBaseQty() + ") cho mặt hàng: " + itemName
                );
            }

            line.recordReceipt(receivedQty, receiverNote);

            // TC-027: If receivedQty > 0, post to receiving branch stock balance and update Moving WAC
            if (receivedQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal receivedLineCost = receivedQty.multiply(line.getSenderUnitCost())
                        .setScale(2, RoundingMode.HALF_UP);

                StockBalance receiverBalance = stockBalanceRepository
                        .findWithLockByBranchIdAndItemId(toBranchId, line.getItemId())
                        .orElse(null);

                if (receiverBalance == null) {
                    receiverBalance = StockBalance.createInitial(
                            toBranch.getBusinessEntityId(),
                            toBranchId,
                            line.getItemId(),
                            receivedQty,
                            line.getSenderUnitCost()
                    );
                } else {
                    receiverBalance.receiveGoods(receivedQty, receivedLineCost);
                }
                stockBalanceRepository.save(receiverBalance);

                // Create immutable TRANSFER_IN movement
                StockMovement inMovement = StockMovement.createTransferInMovement(
                        toBranch.getBusinessEntityId(),
                        toBranchId,
                        line.getItemId(),
                        line.getLotId(),
                        receiptDate,
                        receivedQty,
                        receivedLineCost,
                        line.getSenderUnitCost(),
                        transfer.getId(),
                        transfer.getTransferNumber(),
                        actorUserId
                );
                stockMovementRepository.save(inMovement);
            }
        }

        transfer.completeReceipt(actorUserId);
        StockTransfer saved = stockTransferRepository.save(transfer);
        return toDetailDto(saved, fromBranch, toBranch);
    }

    @Transactional
    public void cancelTransfer(UUID fromBranchId, UUID transferId, UUID actorUserId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        if (!transfer.getFromBranchId().equals(fromBranchId)) {
            throw new IllegalArgumentException("Phiếu chuyển kho không xuất phát từ chi nhánh này.");
        }

        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new InvalidTransferStatusException(
                    "Chỉ có thể hủy phiếu đang ở trạng thái DRAFT. Trạng thái hiện tại: " + transfer.getStatus()
            );
        }

        transfer.cancel(actorUserId);
        stockTransferRepository.save(transfer);
    }

    @Transactional(readOnly = true)
    public TransferDetailDto getTransferDetail(UUID branchId, UUID transferId) {
        StockTransfer transfer = stockTransferRepository.findByIdAndBranch(transferId, branchId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        BranchJpaEntity fromBranch = branchRepository.findById(transfer.getFromBranchId()).orElse(null);
        BranchJpaEntity toBranch = branchRepository.findById(transfer.getToBranchId()).orElse(null);

        return toDetailDto(transfer, fromBranch, toBranch);
    }

    @Transactional(readOnly = true)
    public TransferListResponse getOutboundTransfers(
            UUID fromBranchId,
            TransferStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        Page<StockTransfer> page = stockTransferRepository.findOutboundTransfers(
                fromBranchId, status, startDate, endDate, search, pageable
        );

        List<TransferDetailDto> dtos = page.getContent().stream()
                .map(t -> {
                    BranchJpaEntity from = branchRepository.findById(t.getFromBranchId()).orElse(null);
                    BranchJpaEntity to = branchRepository.findById(t.getToBranchId()).orElse(null);
                    return toDetailDto(t, from, to);
                })
                .toList();

        return new TransferListResponse(dtos, page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }

    @Transactional(readOnly = true)
    public TransferListResponse getInboundTransfers(
            UUID toBranchId,
            TransferStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        Page<StockTransfer> page = stockTransferRepository.findInboundTransfers(
                toBranchId, status, startDate, endDate, search, pageable
        );

        List<TransferDetailDto> dtos = page.getContent().stream()
                .map(t -> {
                    BranchJpaEntity from = branchRepository.findById(t.getFromBranchId()).orElse(null);
                    BranchJpaEntity to = branchRepository.findById(t.getToBranchId()).orElse(null);
                    return toDetailDto(t, from, to);
                })
                .toList();

        return new TransferListResponse(dtos, page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }

    @Transactional(readOnly = true)
    public TransferSummaryDto getTransferSummary(UUID branchId) {
        Pageable unpaged = Pageable.unpaged();
        Page<StockTransfer> outbound = stockTransferRepository.findOutboundTransfers(
                branchId, null, null, null, null, unpaged
        );
        Page<StockTransfer> inbound = stockTransferRepository.findInboundTransfers(
                branchId, null, null, null, null, unpaged
        );

        long outCount = outbound.getTotalElements();
        long inCount = inbound.getTotalElements();

        long inTransit = outbound.getContent().stream()
                .filter(t -> t.getStatus() == TransferStatus.DISPATCHED)
                .count() +
                inbound.getContent().stream()
                .filter(t -> t.getStatus() == TransferStatus.DISPATCHED)
                .count();

        long discrepancies = outbound.getContent().stream()
                .filter(t -> t.getStatus() == TransferStatus.PARTIAL_RECEIPT)
                .count() +
                inbound.getContent().stream()
                .filter(t -> t.getStatus() == TransferStatus.PARTIAL_RECEIPT)
                .count();

        BigDecimal dispatchedCost = outbound.getContent().stream()
                .map(StockTransfer::getTotalDispatchedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal receivedCost = inbound.getContent().stream()
                .map(StockTransfer::getTotalReceivedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransferSummaryDto(
                outCount,
                inCount,
                inTransit,
                discrepancies,
                dispatchedCost,
                receivedCost
        );
    }

    private TransferDetailDto toDetailDto(
            StockTransfer transfer,
            BranchJpaEntity fromBranch,
            BranchJpaEntity toBranch
    ) {
        String fromName = fromBranch != null ? fromBranch.getName() : transfer.getFromBranchId().toString();
        String toName = toBranch != null ? toBranch.getName() : transfer.getToBranchId().toString();

        String reqByName = resolveUserName(transfer.getRequestedBy());
        String dispByName = resolveUserName(transfer.getDispatchedBy());
        String recByName = resolveUserName(transfer.getReceivedBy());

        List<TransferLineDto> lineDtos = transfer.getLines().stream()
                .map(line -> {
                    CatalogItemJpaEntity item = catalogItemRepository.findById(line.getItemId()).orElse(null);
                    String itemName = item != null ? item.getName() : line.getItemId().toString();
                    String itemCode = item != null ? item.getItemCode() : "N/A";
                    String baseUnit = (item != null && item.getBaseUnit() != null) ? item.getBaseUnit().getUnitCode() : "";

                    String lotCode = null;
                    if (line.getLotId() != null) {
                        StockLot lot = stockLotRepository.findById(line.getLotId()).orElse(null);
                        lotCode = lot != null ? lot.getLotCode() : null;
                    }

                    return new TransferLineDto(
                            line.getId(),
                            line.getLineNumber(),
                            line.getItemId(),
                            itemName,
                            itemCode,
                            baseUnit,
                            line.getLotId(),
                            lotCode,
                            line.getDispatchedBaseQty(),
                            line.getReceivedBaseQty(),
                            line.getTransitVarianceQty(),
                            line.getSenderUnitCost(),
                            line.getDispatchedLineCost(),
                            line.getReceivedLineCost(),
                            line.getReceiverNote(),
                            line.getNotes()
                    );
                })
                .toList();

        return new TransferDetailDto(
                transfer.getId(),
                transfer.getTransferNumber(),
                transfer.getBusinessEntityId(),
                transfer.getFromBranchId(),
                fromName,
                transfer.getToBranchId(),
                toName,
                transfer.getRequestedOn(),
                transfer.getDispatchedAt(),
                transfer.getCompletedAt(),
                transfer.getStatus(),
                transfer.getRequestedBy(),
                reqByName,
                transfer.getDispatchedBy(),
                dispByName,
                transfer.getReceivedBy(),
                recByName,
                transfer.getTotalLines(),
                transfer.getTotalDispatchedQuantity(),
                transfer.getTotalReceivedQuantity(),
                transfer.getTotalTransitVarianceQuantity(),
                transfer.getTotalDispatchedCost(),
                transfer.getTotalReceivedCost(),
                transfer.getNotes(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt(),
                lineDtos
        );
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(UserJpaEntity::getDisplayName)
                .orElse("User " + userId);
    }
}
