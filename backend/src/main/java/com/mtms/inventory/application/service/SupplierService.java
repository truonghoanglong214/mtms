package com.mtms.inventory.application.service;

import com.mtms.inventory.application.dto.CreateSupplierCommand;
import com.mtms.inventory.application.dto.SupplierDto;
import com.mtms.inventory.application.exception.DuplicateSupplierCodeException;
import com.mtms.inventory.application.exception.SupplierNotFoundException;
import com.mtms.inventory.domain.Supplier;
import com.mtms.inventory.domain.SupplierRepository;
import com.mtms.inventory.domain.SupplierStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<SupplierDto> getSuppliers(UUID entityId, SupplierStatus status) {
        List<Supplier> suppliers = (status != null)
                ? supplierRepository.findByEntityIdAndStatus(entityId, status)
                : supplierRepository.findByEntityId(entityId);

        return suppliers.stream()
                .map(SupplierDto::fromDomain)
                .toList();
    }

    public SupplierDto getSupplierById(UUID entityId, UUID supplierId) {
        return supplierRepository.findByEntityIdAndId(entityId, supplierId)
                .map(SupplierDto::fromDomain)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
    }

    @Transactional
    public SupplierDto createSupplier(UUID entityId, CreateSupplierCommand command) {
        String normalizedCode = command.supplierCode().trim().toUpperCase();
        if (supplierRepository.existsByEntityIdAndSupplierCode(entityId, normalizedCode)) {
            throw new DuplicateSupplierCodeException(normalizedCode);
        }

        Supplier supplier = new Supplier(
                UUID.randomUUID(),
                entityId,
                normalizedCode,
                command.name().trim(),
                command.contactNote() != null ? command.contactNote().trim() : null,
                SupplierStatus.ACTIVE,
                Instant.now()
        );

        Supplier saved = supplierRepository.save(supplier);
        return SupplierDto.fromDomain(saved);
    }
}
