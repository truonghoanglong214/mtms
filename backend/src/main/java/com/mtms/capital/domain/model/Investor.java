package com.mtms.capital.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Investor {
    private UUID id;
    private UUID entityId;
    private String displayName;
    private String contactPhone;
    private String contactEmail;
    private String identityNumber;
    private String contactNote;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public Investor() {}

    public Investor(UUID id, UUID entityId, String displayName, String contactPhone,
                    String contactEmail, String identityNumber, String contactNote,
                    boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.entityId = entityId;
        this.displayName = displayName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.identityNumber = identityNumber;
        this.contactNote = contactNote;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Investor create(UUID entityId, String displayName, String contactPhone,
                                  String contactEmail, String identityNumber, String contactNote) {
        Instant now = Instant.now();
        return new Investor(UUID.randomUUID(), entityId, displayName, contactPhone,
                contactEmail, identityNumber, contactNote, true, now, now);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getIdentityNumber() { return identityNumber; }
    public void setIdentityNumber(String identityNumber) { this.identityNumber = identityNumber; }

    public String getContactNote() { return contactNote; }
    public void setContactNote(String contactNote) { this.contactNote = contactNote; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
