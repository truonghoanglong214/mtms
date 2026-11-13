package com.mtms.organization.application.exception;

import com.mtms.common.error.ResourceConflictException;

public class DuplicateEntityCodeException extends ResourceConflictException {
    public DuplicateEntityCodeException(String entityCode) {
        super("Business entity with code '" + entityCode + "' already exists");
    }
}
