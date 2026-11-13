package com.mtms.organization.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a registered legal business entity (e.g. Household business or LLC).
 */
public class BusinessEntity {

    private final UUID id;
    private final String entityCode;
    private final String registeredName;
    private final String registrationRef;
    private final BusinessEntityStatus status;
    private final Instant createdAt;

    public BusinessEntity(UUID id,
                          String entityCode,
                          String registeredName,
                          String registrationRef,
                          BusinessEntityStatus status,
                          Instant createdAt) {
        this.id = Objects.requireNonNull(id, "BusinessEntity ID must not be null");
        this.entityCode = validateCode(entityCode);
        this.registeredName = validateName(registeredName);
        this.registrationRef = registrationRef != null ? registrationRef.trim() : null;
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
    }

    private static String validateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity code must not be blank");
        }
        if (code.trim().length() > 32) {
            throw new IllegalArgumentException("Entity code must not exceed 32 characters");
        }
        return code.trim().toUpperCase();
    }

    private static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Registered name must not be blank");
        }
        if (name.trim().length() > 200) {
            throw new IllegalArgumentException("Registered name must not exceed 200 characters");
        }
        return name.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public String getRegisteredName() {
        return registeredName;
    }

    public String getRegistrationRef() {
        return registrationRef;
    }

    public BusinessEntityStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
