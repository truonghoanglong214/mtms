package com.mtms.organization.application.exception;

import com.mtms.common.error.ResourceNotFoundException;

import java.util.UUID;

public class EntityNotFoundException extends ResourceNotFoundException {
    public EntityNotFoundException(String entityType, UUID id) {
        super(entityType + " with ID '" + id + "' not found");
    }
}
