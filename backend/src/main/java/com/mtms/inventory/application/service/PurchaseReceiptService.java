package com.mtms.inventory.application.service;

import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.inventory.application.dto.CreatePurchaseReceiptCommand;
import com.mtms.inventory.application.dto.CreatePurchaseReceiptLineCommand;
import com.mtms.inventory.application.dto.PurchaseReceiptDetailDto;
import com.mtms.inventory.application.dto.PurchaseReceiptDto;
import com.mtms.inventory.application.dto.PurchaseReceiptLineDto;
import com.mtms.inventory.application.dto.PurchaseReceiptListResponse;
import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.application.exception.SupplierNotFoundException;
import com.mtms.inventory.domain.CatalogItem;
import com.mtms.inventory.domain.CatalogItemRepository;
import com.mtms.inventory.domain.InvalidReceiptQuantityException;
import com.mtms.inventory.domain.MissingLotCodeException;
import com.mtms.inventory.domain.Payable;
import com.mtms.inventory.domain.PayableRepository;
import com.mtms.inventory.domain.PaymentStatus;
import com.mtms.inventory.domain.PurchaseReceipt;
import com.mtms.inventory.domain.PurchaseReceiptLine;
import com.mtms.inventory.domain.PurchaseReceiptRepository;
import com.mtms.inventory.domain.ReceiptAlreadyPostedException;
import com.mtms.inventory.domain.ReceiptNotFoundException;
import com.mtms.inventory.domain.ReceiptStatus;
import com.mtms.inventory.domain.StockBalance;
import com.mtms.inventory.domain.StockBalanceRepository;
import com.mtms.inventory.domain.StockLot;
import com.mtms.inventory.domain.StockLotRepository;
import com.mtms.inventory.domain.StockMovement;
import com.mtms.inventory.domain.StockMovementRepository;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.SupplierJpaEntity;
import com.mtms.inventory.infrastructure.persistence.SupplierJpaRepository;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PurchaseReceiptService {

    private final BranchJpaRepository branchRepository;
    private final SupplierJpaRepository supplierRepository;
    private final CatalogItemJpaRepository catalogItemRepository;
    private final UserJpaRepository userRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLotRepository stockLotRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PayableRepository payableRepository;

    public PurchaseReceiptService(
            BranchJpaRepository branchRepository,
            SupplierJpaRepository supplierRepository,
            CatalogItemJpaRepository catalogItemRepository,
            UserJpaRepository userRepository,
            PurchaseReceiptRepository purchaseReceiptRepository,
            StockBalanceRepository stockBalanceRepository,
            StockLotRepository stockLotRepository,
            StockMovementRepository stockMovementRepository,
            PayableRepository payableRepository
    ) {
        this.branchRepository = branchRepository;
        this.supplierRepository = supplierRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.userRepository = userRepository;
        this.purchaseReceiptRepository = purchaseReceiptRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockLotRepository = stockLotRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.payableRepository = payableRepository;
    }

    @Transactional(readOnly = true)
    public PurchaseReceiptListResponse getReceipts(
            UUID branchId,
            ReceiptStatus status,
            UUID supplierId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        if (!branchRepository.existsById(branchId)) {
            throw new BranchNotFoundException(branchId);
        }

        Page<PurchaseReceipt> page = purchaseReceiptRepository.findByBranchIdWithFilters(
                branchId, status, supplierId, startDate, endDate, search, pageable
        );

        List<PurchaseReceiptDto> items = page.getContent().stream()
                .map(this::toReceiptDto)
                .toList();

        return new PurchaseReceiptListResponse(items, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PurchaseReceiptDetailDto getReceiptById(UUID branchId, UUID receiptId) {
        if (!branchRepository.existsById(branchId)) {
            throw new BranchNotFoundException(branchId);
        }

        PurchaseReceipt receipt = purchaseReceiptRepository.findByBranchIdAndId(branchId, receiptId)
                .orElseThrow(() -> new ReceiptNotFoundException(receiptId));

        return toReceiptDetailDto(receipt);
    }

    @Transactional
    public PurchaseReceiptDto createReceipt(UUID branchId, CreatePurchaseReceiptCommand command, UUID actorUserId) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        if (command.supplierId() != null && !supplierRepository.existsById(command.supplierId())) {
            throw new SupplierNotFoundException(command.supplierId());
        }

        // Generate receipt number e.g. PNK-202611-0001
        int year = command.receivedOn().getYear();
        int month = command.receivedOn().getMonthValue();
        long count = purchaseReceiptRepository.countByBranchIdAndMonth(branchId, year, month);
        String receiptNumber = String.format("PNK-%04d%02d-%04d", year, month, count + 1);

        UUID receiptId = UUID.randomUUID();
        List<PurchaseReceiptLine> domainLines = new ArrayList<>();
        BigDecimal runningInvoiceTotal = BigDecimal.ZERO;

        int lineNo = 1;
        for (CreatePurchaseReceiptLineCommand lineCmd : command.lines()) {
            CatalogItemJpaEntity itemEntity = catalogItemRepository.findById(lineCmd.itemId())
                    .orElseThrow(() -> new ItemNotFoundException(lineCmd.itemId()));

            if (!itemEntity.getBusinessEntityId().equals(branch.getBusinessEntityId())) {
                throw new IllegalArgumentException("Item does not belong to branch entity: " + lineCmd.itemId());
            }

            BigDecimal convSnapshot = lineCmd.conversionSnapshot() != null && lineCmd.conversionSnapshot().compareTo(BigDecimal.ZERO) > 0
                    ? lineCmd.conversionSnapshot().setScale(6, RoundingMode.HALF_UP)
                    : BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);

            BigDecimal receivedBaseQty = lineCmd.receivedBaseQuantity();
            if (receivedBaseQty == null) {
                if (lineCmd.declaredPackQuantity() != null && lineCmd.declaredPackQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    receivedBaseQty = lineCmd.declaredPackQuantity().multiply(convSnapshot).setScale(3, RoundingMode.HALF_UP);
                } else {
                    throw new InvalidReceiptQuantityException("Received base quantity must be specified and strictly positive (> 0)");
                }
            }
            if (receivedBaseQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidReceiptQuantityException("Received base quantity must be strictly positive (> 0)");
            }

            BigDecimal unitCost = lineCmd.baseUnitCost() != null
                    ? lineCmd.baseUnitCost().setScale(6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

            BigDecimal lineTotal = lineCmd.lineTotal() != null
                    ? lineCmd.lineTotal().setScale(2, RoundingMode.HALF_UP)
                    : receivedBaseQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

            if (itemEntity.isLotTrackingRequired()) {
                if (lineCmd.lotCode() == null || lineCmd.lotCode().isBlank()) {
                    throw new MissingLotCodeException("Item '" + itemEntity.getItemCode() + "' requires lot tracking but no lot code was provided");
                }
            }

            PurchaseReceiptLine line = new PurchaseReceiptLine(
                    UUID.randomUUID(),
                    receiptId,
                    lineNo++,
                    lineCmd.itemId(),
                    lineCmd.packName(),
                    lineCmd.declaredPackQuantity(),
                    convSnapshot,
                    receivedBaseQty,
                    unitCost,
                    lineTotal,
                    lineCmd.lotCode(),
                    lineCmd.expiresOn()
            );
            domainLines.add(line);
            runningInvoiceTotal = runningInvoiceTotal.add(lineTotal);
        }

        PaymentStatus paymentStatus = command.paymentStatus() != null ? command.paymentStatus() : PaymentStatus.UNPAID;

        PurchaseReceipt receipt = new PurchaseReceipt(
                receiptId,
                branch.getBusinessEntityId(),
                branchId,
                receiptNumber,
                command.supplierId(),
                command.receivedOn(),
                command.invoiceNumber(),
                runningInvoiceTotal.setScale(2, RoundingMode.HALF_UP),
                paymentStatus,
                null,
                ReceiptStatus.DRAFT,
                command.notes(),
                null,
                null,
                null,
                domainLines
        );

        receipt = purchaseReceiptRepository.save(receipt);

        if (Boolean.TRUE.equals(command.autoPost())) {
            receipt = postReceiptInternal(receipt, branch, command.dueDate(), actorUserId);
        }

        return toReceiptDto(receipt);
    }

    @Transactional
    public PurchaseReceiptDto postReceipt(UUID branchId, UUID receiptId, UUID actorUserId) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        PurchaseReceipt receipt = purchaseReceiptRepository.findByBranchIdAndId(branchId, receiptId)
                .orElseThrow(() -> new ReceiptNotFoundException(receiptId));

        if (receipt.getStatus() == ReceiptStatus.POSTED) {
            throw new ReceiptAlreadyPostedException("Receipt has already been posted: " + receipt.getReceiptNumber());
        }

        receipt = postReceiptInternal(receipt, branch, null, actorUserId);
        return toReceiptDto(receipt);
    }

    private PurchaseReceipt postReceiptInternal(
            PurchaseReceipt receipt,
            BranchJpaEntity branch,
            LocalDate dueDate,
            UUID actorUserId
    ) {
        // 1. Mark receipt as POSTED
        receipt.post(actorUserId);

        // 2. Process each receipt line: Update StockBalance, StockLot, StockMovement
        for (PurchaseReceiptLine line : receipt.getLines()) {
            UUID itemId = line.getItemId();
            BigDecimal receivedQty = line.getReceivedBaseQuantity();
            BigDecimal lineTotal = line.getLineTotal();
            BigDecimal baseUnitCost = line.getBaseUnitCost();

            // A. Update Stock Balance & Moving WAC
            Optional<StockBalance> existingBalanceOpt = stockBalanceRepository.findByBranchIdAndItemId(branch.getId(), itemId);
            if (existingBalanceOpt.isPresent()) {
                StockBalance balance = existingBalanceOpt.get();
                balance.receiveGoods(receivedQty, lineTotal);
                stockBalanceRepository.save(balance);
            } else {
                StockBalance newBalance = StockBalance.createInitial(
                        branch.getBusinessEntityId(),
                        branch.getId(),
                        itemId,
                        receivedQty,
                        baseUnitCost
                );
                newBalance.setInventoryValue(lineTotal);
                stockBalanceRepository.save(newBalance);
            }

            // B. Lot Tracking (if lotCode provided)
            UUID lotId = null;
            if (line.getLotCode() != null && !line.getLotCode().isBlank()) {
                Optional<StockLot> lotOpt = stockLotRepository.findByBranchIdAndItemIdAndLotCode(
                        branch.getId(), itemId, line.getLotCode()
                );
                if (lotOpt.isPresent()) {
                    StockLot lot = lotOpt.get();
                    lot.setQuantity(lot.getQuantity().add(receivedQty));
                    lot = stockLotRepository.save(lot);
                    lotId = lot.getId();
                } else {
                    StockLot newLot = StockLot.createInitial(
                            branch.getBusinessEntityId(),
                            branch.getId(),
                            itemId,
                            line.getLotCode(),
                            line.getExpiresOn(),
                            receivedQty
                    );
                    newLot = stockLotRepository.save(newLot);
                    lotId = newLot.getId();
                }
            }

            // C. Stock Movement (Immutable Audit Ledger)
            StockMovement movement = StockMovement.createPurchaseReceiptMovement(
                    branch.getBusinessEntityId(),
                    branch.getId(),
                    itemId,
                    lotId,
                    receipt.getReceivedOn(),
                    receivedQty,
                    lineTotal,
                    baseUnitCost,
                    receipt.getId(),
                    receipt.getReceiptNumber(),
                    actorUserId
            );
            stockMovementRepository.save(movement);
        }

        // 3. TC-008 Invariant: If UNPAID, create payable debt in mtms.payables
        if (receipt.getPaymentStatus() == PaymentStatus.UNPAID) {
            String supplierName = "Nhà cung cấp";
            if (receipt.getSupplierId() != null) {
                supplierName = supplierRepository.findById(receipt.getSupplierId())
                        .map(SupplierJpaEntity::getName)
                        .orElse("Nhà cung cấp");
            }

            LocalDate debtDueDate = dueDate != null ? dueDate : receipt.getReceivedOn().plusDays(30);

            Payable payable = Payable.createPurchaseReceiptDebt(
                    branch.getBusinessEntityId(),
                    branch.getId(),
                    supplierName,
                    receipt.getId(),
                    receipt.getReceivedOn(),
                    debtDueDate,
                    receipt.getInvoiceTotal()
            );
            payable = payableRepository.save(payable);
            receipt.setPayableId(payable.getId());
        }

        // 4. Save finalized receipt
        return purchaseReceiptRepository.save(receipt);
    }

    private PurchaseReceiptDto toReceiptDto(PurchaseReceipt receipt) {
        String supplierName = null;
        if (receipt.getSupplierId() != null) {
            supplierName = supplierRepository.findById(receipt.getSupplierId())
                    .map(SupplierJpaEntity::getName)
                    .orElse(null);
        }

        String postedByName = null;
        if (receipt.getPostedBy() != null) {
            postedByName = userRepository.findById(receipt.getPostedBy())
                    .map(UserJpaEntity::getDisplayName)
                    .orElse(receipt.getPostedBy().toString());
        }

        return new PurchaseReceiptDto(
                receipt.getId(),
                receipt.getBranchId(),
                receipt.getReceiptNumber(),
                receipt.getSupplierId(),
                supplierName,
                receipt.getReceivedOn(),
                receipt.getInvoiceNumber(),
                receipt.getInvoiceTotal(),
                receipt.getPaymentStatus(),
                receipt.getPayableId(),
                receipt.getStatus(),
                receipt.getLines().size(),
                postedByName,
                receipt.getPostedAt()
        );
    }

    private PurchaseReceiptDetailDto toReceiptDetailDto(PurchaseReceipt receipt) {
        String supplierName = null;
        if (receipt.getSupplierId() != null) {
            supplierName = supplierRepository.findById(receipt.getSupplierId())
                    .map(SupplierJpaEntity::getName)
                    .orElse(null);
        }

        String postedByName = null;
        if (receipt.getPostedBy() != null) {
            postedByName = userRepository.findById(receipt.getPostedBy())
                    .map(UserJpaEntity::getDisplayName)
                    .orElse(receipt.getPostedBy().toString());
        }

        List<PurchaseReceiptLineDto> lineDtos = receipt.getLines().stream()
                .map(line -> {
                    String itemCode = "";
                    String itemName = "";
                    String baseUnitName = "";

                    Optional<CatalogItemJpaEntity> itemOpt = catalogItemRepository.findById(line.getItemId());
                    if (itemOpt.isPresent()) {
                        CatalogItemJpaEntity item = itemOpt.get();
                        itemCode = item.getItemCode();
                        itemName = item.getName();
                        if (item.getBaseUnit() != null) {
                            baseUnitName = item.getBaseUnit().getUnitName();
                        }
                    }

                    return new PurchaseReceiptLineDto(
                            line.getId(),
                            line.getLineNo(),
                            line.getItemId(),
                            itemCode,
                            itemName,
                            baseUnitName,
                            line.getPackName(),
                            line.getDeclaredPackQuantity(),
                            line.getConversionSnapshot(),
                            line.getReceivedBaseQuantity(),
                            line.getBaseUnitCost(),
                            line.getLineTotal(),
                            line.getLotCode(),
                            line.getExpiresOn()
                    );
                })
                .toList();

        return new PurchaseReceiptDetailDto(
                receipt.getId(),
                receipt.getBranchId(),
                receipt.getBusinessEntityId(),
                receipt.getReceiptNumber(),
                receipt.getSupplierId(),
                supplierName,
                receipt.getReceivedOn(),
                receipt.getInvoiceNumber(),
                receipt.getInvoiceTotal(),
                receipt.getPaymentStatus(),
                receipt.getPayableId(),
                receipt.getStatus(),
                receipt.getNotes(),
                receipt.getPostedBy(),
                postedByName,
                receipt.getPostedAt(),
                receipt.getCreatedAt(),
                receipt.getLines().size(),
                lineDtos
        );
    }
}
