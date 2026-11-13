package com.mtms.organization.application.dto;

import com.mtms.organization.domain.BusinessEntityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBusinessEntityCommand(
        @NotBlank(message = "Entity code must not be blank")
        @Size(max = 32, message = "Entity code must not exceed 32 characters")
        String entityCode,

        @NotBlank(message = "Registered name must not be blank")
        @Size(max = 200, message = "Registered name must not exceed 200 characters")
        String registeredName,

        @Size(max = 100, message = "Registration reference must not exceed 100 characters")
        String registrationRef,

        @NotNull(message = "Status must be specified")
        BusinessEntityStatus status
) {
}
