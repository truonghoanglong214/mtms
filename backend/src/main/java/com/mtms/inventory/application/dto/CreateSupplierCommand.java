package com.mtms.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSupplierCommand(
        @NotBlank(message = "Supplier code must not be blank")
        @Size(min = 3, max = 50, message = "Supplier code must be 3-50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Supplier code must contain only alphanumeric characters, dashes or underscores")
        String supplierCode,

        @NotBlank(message = "Supplier name must not be blank")
        @Size(max = 200, message = "Supplier name must be at most 200 characters")
        String name,

        String contactNote
) {
}
