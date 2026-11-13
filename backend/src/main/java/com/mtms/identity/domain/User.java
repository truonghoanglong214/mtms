package com.mtms.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing an administrative user with login credentials.
 */
public class User {

    private final UUID id;
    private final String loginIdentifier;
    private final String passwordHash;
    private final String displayName;
    private final UserStatus status;
    private final Instant createdAt;

    public User(UUID id,
                String loginIdentifier,
                String passwordHash,
                String displayName,
                UserStatus status,
                Instant createdAt) {
        this.id = Objects.requireNonNull(id, "User ID must not be null");
        this.loginIdentifier = validateLoginIdentifier(loginIdentifier);
        this.passwordHash = validatePasswordHash(passwordHash);
        this.displayName = validateDisplayName(displayName);
        this.status = Objects.requireNonNull(status, "User status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
    }

    private static String validateLoginIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Login identifier must not be blank");
        }
        if (identifier.trim().length() > 200) {
            throw new IllegalArgumentException("Login identifier must not exceed 200 characters");
        }
        return identifier.trim().toLowerCase();
    }

    private static String validatePasswordHash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        return hash.trim();
    }

    private static String validateDisplayName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name must not be blank");
        }
        if (name.trim().length() > 160) {
            throw new IllegalArgumentException("Display name must not exceed 160 characters");
        }
        return name.trim();
    }

    public boolean canLogin() {
        return this.status == UserStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getLoginIdentifier() {
        return loginIdentifier;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
