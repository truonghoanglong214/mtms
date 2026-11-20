package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.UnitDimension;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUnitCommand(
        @NotBlank(message = "Unit code must not be blank")
        @Size(max = 20, message = "Unit code must be at most 20 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Unit code must contain only alphanumeric characters, dashes or underscores")
        String unitCode,

        @NotBlank(message = "Unit name must not be blank")
        @Size(max = 70, message = "Unit name must be at most 70 characters")
        String unitName,

        @NotNull(message = "Dimension must not be null")
        UnitDimension dimension
) {
}
