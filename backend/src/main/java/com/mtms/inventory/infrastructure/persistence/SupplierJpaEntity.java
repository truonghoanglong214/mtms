package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.Supplier;
import com.mtms.inventory.domain.SupplierStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suppliers", schema = "mtms")
public class SupplierJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "contact_note", columnDefinition = "TEXT")
    private String contactNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupplierStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SupplierJpaEntity() {
    }

    public SupplierJpaEntity(
            UUID id,
            UUID businessEntityId,
            String supplierCode,
            String name,
            String contactNote,
            SupplierStatus status,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.businessEntityId = businessEntityId;
        this.supplierCode = supplierCode;
        this.name = name;
        this.contactNote = contactNote;
        this.status = status != null ? status : SupplierStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static SupplierJpaEntity fromDomain(Supplier domain) {
        return new SupplierJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getSupplierCode(),
                domain.getName(),
                domain.getContactNote(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }

    public Supplier toDomain() {
        return new Supplier(
                this.id,
                this.businessEntityId,
                this.supplierCode,
                this.name,
                this.contactNote,
                this.status,
                this.createdAt
        );
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
}
