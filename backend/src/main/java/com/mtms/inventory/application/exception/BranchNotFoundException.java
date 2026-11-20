package com.mtms.inventory.application.exception;

import java.util.UUID;

public class BranchNotFoundException extends RuntimeException {
    public BranchNotFoundException(UUID branchId) {
        super("Branch with ID " + branchId + " not found");
    }
}
