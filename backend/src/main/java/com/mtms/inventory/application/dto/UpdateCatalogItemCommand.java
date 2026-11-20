package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCatalogItemCommand(
        @NotBlank(message = "Item name must not be blank")
        @Size(max = 160, message = "Item name must be at most 160 characters")
        String name,

        @NotNull(message = "Item kind must not be null")
        ItemKind itemKind,

        boolean lotTrackingRequired,

        boolean expirationTrackingRequired,

        @NotNull(message = "Status must not be null")
        ItemStatus status
) {
}
