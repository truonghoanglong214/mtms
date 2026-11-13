package com.mtms.organization.application.exception;

import com.mtms.common.error.ResourceConflictException;

import java.util.UUID;

public class DuplicateBranchCodeException extends ResourceConflictException {
    public DuplicateBranchCodeException(UUID entityId, String branchCode) {
        super("Branch with code '" + branchCode + "' already exists under business entity '" + entityId + "'");
    }
}
