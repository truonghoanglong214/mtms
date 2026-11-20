package com.mtms.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Supplier {

    private final UUID id;
    private final UUID businessEntityId;
    private final String supplierCode;
    private final String name;
    private final String contactNote;
    private final SupplierStatus status;
    private final Instant createdAt;

    public Supplier(UUID id, UUID businessEntityId, String supplierCode, String name, String contactNote, SupplierStatus status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Supplier ID must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "Business entity ID must not be null");
        this.supplierCode = Objects.requireNonNull(supplierCode, "Supplier code must not be null").trim();
        this.name = Objects.requireNonNull(name, "Supplier name must not be null").trim();
        this.contactNote = contactNote;
        this.status = status != null ? status : SupplierStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getName() {
        return name;
    }

    public String getContactNote() {
        return contactNote;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Supplier supplier)) return false;
        return Objects.equals(id, supplier.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
