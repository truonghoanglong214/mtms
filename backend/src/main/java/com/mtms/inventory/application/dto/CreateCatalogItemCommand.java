package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.ItemKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCatalogItemCommand(
        @NotBlank(message = "Item code must not be blank")
        @Size(min = 3, max = 50, message = "Item code must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Z0-9_-]{3,50}$", message = "Item code must consist of 3-50 uppercase alphanumeric characters, underscores or hyphens")
        String itemCode,

        @NotBlank(message = "Item name must not be blank")
        @Size(max = 160, message = "Item name must be at most 160 characters")
        String name,

        @NotNull(message = "Base unit ID must not be null")
        UUID baseUnitId,

        @NotNull(message = "Item kind must not be null")
        ItemKind itemKind,

        boolean lotTrackingRequired,

        boolean expirationTrackingRequired
) {
}
