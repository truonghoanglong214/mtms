package com.mtms.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command for registering a new dedicated branch bank account.
 */
public record CreateBankAccountCommand(
        @NotBlank(message = "Bank name must not be blank")
        @Size(max = 160, message = "Bank name must not exceed 160 characters")
        String bankName,

        @NotBlank(message = "Account holder name must not be blank")
        @Size(max = 160, message = "Account holder name must not exceed 160 characters")
        String accountHolderName,

        @NotBlank(message = "Account number must not be blank")
        @Size(min = 4, max = 50, message = "Account number must be between 4 and 50 characters")
        String accountNumber,

        Boolean isPrimary
) {
    public boolean primary() {
        return Boolean.TRUE.equals(isPrimary);
    }
}
