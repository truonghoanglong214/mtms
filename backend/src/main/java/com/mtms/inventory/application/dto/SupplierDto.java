package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.Supplier;
import com.mtms.inventory.domain.SupplierStatus;

import java.time.Instant;
import java.util.UUID;

public record SupplierDto(
        UUID id,
        UUID businessEntityId,
        String supplierCode,
        String name,
        String contactNote,
        SupplierStatus status,
        Instant createdAt
) {
    public static SupplierDto fromDomain(Supplier supplier) {
        return new SupplierDto(
                supplier.getId(),
                supplier.getBusinessEntityId(),
                supplier.getSupplierCode(),
                supplier.getName(),
                supplier.getContactNote(),
                supplier.getStatus(),
                supplier.getCreatedAt()
        );
    }
}
