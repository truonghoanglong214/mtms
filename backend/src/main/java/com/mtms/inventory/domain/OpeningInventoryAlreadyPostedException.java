package com.mtms.inventory.domain;

/**
 * P0-INV-02 / BR-INV-08 / TC-024
 * Thrown when an opening inventory commit is attempted on a branch that has already been posted and locked.
 */
public class OpeningInventoryAlreadyPostedException extends RuntimeException {
    public OpeningInventoryAlreadyPostedException(String message) {
        super(message);
    }
}
