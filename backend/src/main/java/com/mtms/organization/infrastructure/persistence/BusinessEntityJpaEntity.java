package com.mtms.organization.infrastructure.persistence;

import com.mtms.organization.domain.BusinessEntityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_entities", schema = "mtms")
public class BusinessEntityJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entity_code", nullable = false, unique = true, length = 32)
    private String entityCode;

    @Column(name = "registered_name", nullable = false, length = 200)
    private String registeredName;

    @Column(name = "registration_ref", length = 100)
    private String registrationRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BusinessEntityStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BusinessEntityJpaEntity() {
    }

    public BusinessEntityJpaEntity(UUID id,
                                  String entityCode,
                                  String registeredName,
                                  String registrationRef,
                                  BusinessEntityStatus status,
                                  Instant createdAt) {
        this.id = id;
        this.entityCode = entityCode;
        this.registeredName = registeredName;
        this.registrationRef = registrationRef;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public String getRegisteredName() {
        return registeredName;
    }

    public void setRegisteredName(String registeredName) {
        this.registeredName = registeredName;
    }

    public String getRegistrationRef() {
        return registrationRef;
    }

    public void setRegistrationRef(String registrationRef) {
        this.registrationRef = registrationRef;
    }

    public BusinessEntityStatus getStatus() {
        return status;
    }

    public void setStatus(BusinessEntityStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
